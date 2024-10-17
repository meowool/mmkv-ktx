/*
 * Copyright (C) 2024 Meowool <https://github.com/meowool/mmkv-ktx/graphs/contributors>
 *
 * This file is part of the MMKV-KTX project <https://github.com/meowool/mmkv-ktx>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.meowool.mmkv.ktx.compiler.codegen

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.meowool.mmkv.ktx.compiler.Names
import com.meowool.mmkv.ktx.compiler.Names.BuiltInConverters
import com.meowool.mmkv.ktx.compiler.Names.MMKV
import com.meowool.mmkv.ktx.compiler.Names.MutableStateFlow
import com.meowool.mmkv.ktx.compiler.Names.Parcelable
import com.meowool.mmkv.ktx.compiler.Names.PersistDefaultValue
import com.meowool.mmkv.ktx.compiler.Names.Preferences
import com.meowool.mmkv.ktx.compiler.Names.addInvisibleSuppress
import com.meowool.mmkv.ktx.compiler.Names.mapState
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.LambdaTypeName.Companion.get as lambdaType
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy as by

class PreferencesImplClasses : CodegenStep() {
  override fun generate() = context.preferences.process(::generatePreferences)

  private fun generatePreferences(preferences: KSClassDeclaration) {
    val dataClassName = preferences.toClassName()
    val className = context.preferencesImplClassName(preferences)
    val mutableClassName = context.mutableClassName(preferences)
    val mutableImplClassName = context.mutableImplClassName(preferences)
    val annotation = requireNotNull(preferences.findAnnotation(Preferences))
    val id = annotation.findStringArgument("id") ?: preferences.simpleName.asString()
    val expires = annotation.findIntArgument("expires") ?: -1
    val cryptKey = annotation.findStringArgument("cryptKey") ?: ""

    val mmkvPropertySpec = PropertySpec.builder("mmkv", MMKV)
      .addModifiers(KModifier.OVERRIDE)
      .initializer(buildCodeBlock {
        add("%T.mmkvWithID(%S", MMKV, id)
        if (cryptKey.isNotEmpty()) {
          add(", %M, %S)", MMKV.member("SINGLE_PROCESS_MODE"), cryptKey)
        } else {
          add(")")
        }
        if (expires > -1) {
          beginControlFlow(".apply·{")
          addStatement("enableAutoKeyExpire(%L)", expires)
          endControlFlow()
        }
      })
      .build()

    val defaultPropertySpec = PropertySpec.builder("default", dataClassName)
      .addModifiers(KModifier.PRIVATE)
      .initializer("%T()", dataClassName)
      .build()

    val dataClassNullable = dataClassName.copy(nullable = true)
    val instancePropertySpec = PropertySpec.builder("_instance", dataClassNullable)
      .mutable()
      .initializer("null")
      .addModifiers(KModifier.PRIVATE)
      .addAnnotation(Volatile::class)
      .build()

    val mutableStateFlowNullable = MutableStateFlow.by(dataClassName).copy(nullable = true)
    val statePropertySpec = PropertySpec.builder("_state", mutableStateFlowNullable)
      .mutable()
      .initializer("null")
      .addModifiers(KModifier.PRIVATE)
      .addAnnotation(Volatile::class)
      .build()

    val getFunSpec = FunSpec.builder("get")
      .addModifiers(KModifier.OVERRIDE)
      .returns(dataClassName)
      .addCode(buildCodeBlock {
        addStatement("val instance = %N", instancePropertySpec)
        addStatement("if (instance != null) return instance\n")
        beginControlFlow("return synchronized(this)")
        addStatement("val instance = %N", instancePropertySpec)
        addStatement("if (instance != null) return instance\n")
        addStatement("%T(", dataClassName)
        indent()
        preferences.mapDecodeAssignments()?.forEach(::add)
        unindent()
        addStatement(").also·{·%N·=·it·}", instancePropertySpec)
        endControlFlow()
      })
      .build()

    val mutableFunSpec = FunSpec.builder("mutable")
      .addModifiers(KModifier.OVERRIDE)
      .returns(mutableClassName)
      .addStatement("return %L()", mutableImplClassName.simpleName)
      .build()

    val updateFunSpec = FunSpec.builder("update")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter("mutable", mutableClassName)
      .addStatement("val new = mutable.toImmutable()")
      .addStatement("%N?.value = new", statePropertySpec)
      .addStatement("synchronized(this) { %N = new }", instancePropertySpec)
      .build()

    val asStateFlowFunSpec = FunSpec.builder("asStateFlow")
      .addModifiers(KModifier.OVERRIDE)
      .returns(MutableStateFlow.by(dataClassName))
      .addCode(buildCodeBlock {
        addStatement("val state = %N", statePropertySpec)
        addStatement("if (state != null) return state\n")
        beginControlFlow("return synchronized(this)")
        addStatement("val state = %N", statePropertySpec)
        addStatement("if (state != null) return state\n")
        addStatement(
          "%T(get()).also·{·%N·=·it·}",
          MutableStateFlow.by(dataClassName), statePropertySpec
        )
        endControlFlow()
      })
      .build()

    val mapStateFlowSpec = FunSpec.builder("mapStateFlow")
      .addModifiers(KModifier.OVERRIDE)
      .addTypeVariable(TypeVariableName("R"))
      .addParameter(
        name = "transform",
        type = lambdaType(
          parameters = arrayOf(preferences.toClassName()),
          returnType = TypeVariableName("R")
        )
      )
      .returns(Names.StateFlow.parameterizedBy(TypeVariableName("R")))
      .addStatement("return·asStateFlow().%M(transform)", mapState)
      .build()

    val mutableImplClassBuilder = TypeSpec.classBuilder(mutableImplClassName)
      .addModifiers(KModifier.INNER, KModifier.PRIVATE)
      .addSuperinterface(mutableClassName)
      .addProperty(
        PropertySpec.builder("old", dataClassName)
          .addModifiers(KModifier.PRIVATE)
          .initializer("get()")
          .build()
      )
      .addFunction(
        FunSpec.builder("toImmutable")
          .addModifiers(KModifier.OVERRIDE)
          .returns(dataClassName)
          .addCode(buildCodeBlock {
            add("return·")
            addStatement("%T(", dataClassName)
            indent()
            preferences.primaryConstructor?.parameters?.forEach { parameter ->
              val name = requireNotNull(parameter.name?.asString())
              addStatement("%L = %L,", name, name)
            }
            unindent()
            addStatement(")")
          })
          .build()
      )

    preferences.primaryConstructor?.parameters?.forEach { parameter ->
      val name = requireNotNull(parameter.name?.asString())
      val type = parameter.type

      mutableImplClassBuilder.addProperty(
        PropertySpec.builder(name, type.toTypeName())
          .addModifiers(KModifier.OVERRIDE)
          .initializer("old.%L", name)
          .setter(
            FunSpec.setterBuilder()
              .addParameter("new", type.toTypeName())
              .addCode(buildCodeBlock {
                addStatement("field = new")
                addEncodeStatement(name, type, preferences, "new")
              })
              .build()
          )
          .mutable()
          .build()
      )
    }

    val classSpec = TypeSpec.classBuilder(className)
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameters(context.classTypeConvertersParams)
          .build()
      )
      .addModifiers(KModifier.INTERNAL)
      .addSuperinterface(context.preferencesClassName(preferences))
      .addAnnotation(PublishedApi::class)
      .apply {
        context.classTypeConvertersParams.forEach {
          addProperty(
            PropertySpec.builder(it.name, it.type)
              .initializer(it.name)
              .addModifiers(KModifier.PRIVATE)
              .build()
          )
        }
      }
      .addProperty(mmkvPropertySpec)
      .addProperty(defaultPropertySpec)
      .addProperty(instancePropertySpec)
      .addProperty(statePropertySpec)
      .addFunction(getFunSpec)
      .addFunction(mutableFunSpec)
      .addFunction(updateFunSpec)
      .addFunction(asStateFlowFunSpec)
      .addFunction(mapStateFlowSpec)
      .addType(mutableImplClassBuilder.build())
      .build()

    FileSpec.builder(className)
      .addAnnotation(
        AnnotationSpec.builder(Suppress::class)
          .addInvisibleSuppress()
          .addMember("%S", "NAME_SHADOWING")
          .build()
      )
      .addType(classSpec)
      .build()
      .write(preferences)
  }

  private fun KSClassDeclaration.mapDecodeAssignments() = primaryConstructor?.parameters?.map {
    require(!it.isVararg) {
      "[${this.logName()}] " +
        "Vararg is not supported for property '${it.name?.asString()}', " +
        "consider replacing it with a supported one."
    }
    require(it.hasDefault) {
      "[${this.logName()}] " +
        "Property '${it.name?.asString()}' must have a default value, " +
        "consider adding an initial value to it."
    }
    val preferences = this
    val name = requireNotNull(it.name?.asString())
    val isPersistent = it.findAnnotation(PersistDefaultValue) != null
    val type = it.type
    val resolvedType = type.resolve()
    val declaration = requireNotNull(resolvedType.findActualDeclaration()) {
      "[${this.logName()}] " +
        "Cannot find actual declaration of the type ${type.logName()} for property '$name', " +
        "this may be due to the type is not supported, " +
        "consider replacing it with a supported one."
    }
    when (val primitive = resolveMMKVType(type, declaration)) {
      null -> when {
        Modifier.ENUM in declaration.modifiers -> buildCodeBlock {
          addStatement("$name·=·run·{")
          indent()
          addInitializationStatement(isPersistent, name, type, preferences, returnAdvanced = false)
          addStatement("val·ordinal·=·mmkv.decodeInt(%S,·-1)", name)
          addStatement("if·(ordinal·==·-1)·default.$name")
          addStatement("else %T.entries[ordinal]", declaration.toClassName())
          unindent()
          addStatement("},")
        }
        declaration.superTypes.contains(Parcelable) -> buildCodeBlock {
          addStatement("$name·=·run·{")
          indent()
          addInitializationStatement(isPersistent, name, type, preferences)
          addStatement(
            "mmkv.decodeParcelable(%S,·%T::class.java)·?:·default.$name",
            name, declaration.toClassName()
          )
          unindent()
          addStatement("},")
        }
        else -> {
          val typeConverter = requireNotNull(resolvedType.findTypeConverter(this)) {
            "[${this.logName()}] " +
              "Unsupported type '${type.logName()}' for property '$name', " +
              "consider replacing it with a supported one or creating a type converter."
          }

          buildCodeBlock {
            addStatement("$name·=·run·{")
            indent()
            addInitializationStatement(isPersistent, name, type, preferences)
            typeConverter.decoderProvider?.let { provider ->
              addStatement("with($provider)·{")
              indent()
              when (typeConverter.encodeType) {
                "Bytes", "String", "StringSet" -> addStatement(
                  "mmkv.decode%L(%S)?.%L() ?: default.$name",
                  typeConverter.encodeType, name, typeConverter.decoderName.simpleName,
                )
                else -> addStatement(
                  "mmkv.decode%L(%S).%L()",
                  typeConverter.encodeType, name, typeConverter.decoderName.simpleName
                )
              }
            } ?: run {
              when (typeConverter.encodeType) {
                "Bytes", "String", "StringSet" -> addStatement(
                  "mmkv.decode%L(%S)?.%M() ?: default.$name",
                  typeConverter.encodeType, name, typeConverter.decoderName
                )
                else -> addStatement(
                  "mmkv.decode%L(%S).%M()",
                  typeConverter.encodeType, name, typeConverter.decoderName
                )
              }
            }
            typeConverter.decoderProvider?.let {
              unindent()
              addStatement("}")
            }
            unindent()
            addStatement("},")
          }
        }
      }
      else -> buildCodeBlock {
        addStatement("$name·=·run·{")
        indent()
        addInitializationStatement(isPersistent, name, type, preferences)
        when {
          primitive == "Bytes" || primitive == "String" || primitive == "StringSet" ->
            addStatement("mmkv.decode%L(%S)·?:·default.$name", primitive, name)

          // See `BuiltInConverters.kt`
          resolvedType.isMarkedNullable -> when (primitive) {
            "Bool" -> addStatement(
              "%T.decodeNullableBoolean(mmkv.decodeInt(%S,·-1))",
              BuiltInConverters, name
            )
            "Int", "Float" -> addStatement(
              "%T.decodeNullable$primitive(mmkv.decodeLong(%S,·Long.MIN_VALUE))",
              BuiltInConverters, name
            )
            else -> addStatement(
              "%T.decodeNullable%L(mmkv.decodeBytes(%S))",
              BuiltInConverters, primitive, name
            )
          }

          else -> addStatement("mmkv.decode$primitive(%S,·default.$name)", name)
        }
        unindent()
        addStatement("},")
      }
    }
  }

  private fun CodeBlock.Builder.addInitializationStatement(
    isPersistent: Boolean,
    name: String,
    type: KSTypeReference,
    preferences: KSClassDeclaration,
    returnAdvanced: Boolean = true,
  ) {
    if (isPersistent) {
      add("if·(!mmkv.containsKey(%S))·", name)
      if (returnAdvanced) {
        addStatement("{")
        indent()
      }
      addEncodeStatement(name, type, preferences, "default.$name")
      if (returnAdvanced) {
        addStatement("return@run·default.$name")
        unindent()
        addStatement("}")
      }
    }
  }

  private fun CodeBlock.Builder.addEncodeStatement(
    name: String,
    type: KSTypeReference,
    preferences: KSClassDeclaration,
    value: String,
  ) {
    val resolvedType = type.resolve()
    val declaration = requireNotNull(resolvedType.findActualDeclaration())
    val primitive = resolveMMKVType(type, declaration)
    when {
      primitive != null && resolvedType.isMarkedNullable -> when (primitive) {
        "Bool" -> addStatement(
          "mmkv.encode(%S,·%T.encodeNullableBoolean($value))",
          name, BuiltInConverters
        )
        "Bytes", "String", "StringSet" -> addStatement("mmkv.encode(%S,·$value)", name)
        else -> addStatement(
          "mmkv.encode(%S,·%T.encodeNullable%L($value))",
          name, BuiltInConverters, primitive
        )
      }

      primitive != null || declaration.superTypes.contains(Parcelable) ->
        addStatement("mmkv.encode(%S, $value)", name)

      // Modifier.ENUM in declaration.modifiers -> addStatement(
      //   when (resolvedType.isMarkedNullable) {
      //     true -> "mmkv.encode(%S,·$value?.ordinal·?:·-1)"
      //     false -> "mmkv.encode(%S,·$value.ordinal)"
      //   },
      //   name
      // )

      else -> {
        val typeConverter = requireNotNull(resolvedType.findTypeConverter(declaration)) {
          "[${preferences.logName()}] " +
            "Unsupported type '${type.logName()}' for property '$name', " +
            "consider replacing it with a supported one or creating a type converter."
        }
        typeConverter.encoderProvider?.let { provider ->
          addStatement("with($provider)·{")
          indent()
          addStatement(
            "mmkv.encode(%S,·$value.%L())",
            name, typeConverter.encoderName.simpleName,
          )
        } ?: run {
          addStatement(
            "mmkv.encode(%S,·$value.%M())",
            name, typeConverter.encoderName,
          )
        }
        typeConverter.encoderProvider?.let {
          unindent()
          addStatement("}")
        }
      }
    }
  }

  private fun resolveMMKVType(
    raw: KSTypeReference?,
    declaration: KSClassDeclaration,
  ) = when (declaration.toClassName()) {
    BOOLEAN -> "Bool"
    INT -> "Int"
    LONG -> "Long"
    FLOAT -> "Float"
    DOUBLE -> "Double"
    BYTE_ARRAY -> "Bytes"
    STRING -> "String"
    SET -> when {
      raw?.element?.typeArguments?.singleOrNull()?.type.matches(STRING) -> "StringSet"
      else -> null
    }
    else -> null
  }

  private fun KSType.findTypeConverter(original: KSDeclaration): TypeConverter? {
    val typeConverters = context.typeConverters.flatMap(KSClassDeclaration::getDeclaredFunctions)
    val encoder = typeConverters.filter(KSDeclaration::isPublic).firstOrNull {
      it.parameters.isEmpty() &&
        it.extensionReceiver.matches(this) &&
        it.extensionReceiver?.resolve().matchesNullable(this)
    }
    val decoder = typeConverters.filter(KSDeclaration::isPublic).firstOrNull {
      it.parameters.isEmpty() &&
        it.returnType.matches(this) &&
        it.returnType?.resolve().matchesNullable(this) &&
        it.extensionReceiver.matches(encoder?.returnType) &&
        it.extensionReceiver?.resolve().matchesNullable(encoder?.returnType?.resolve())
    }
    if (encoder == null || decoder == null) return null

    val encodeDeclaration = requireNotNull(encoder.returnType?.resolve()?.findActualDeclaration()) {
      "[${original.logName()}] " +
        "Cannot find actual declaration of the type ${encoder.returnType.logName()} " +
        "for type converter '${encoder.logName()}', " +
        "this may be due to the type is not supported, " +
        "consider replacing it with a supported one."
    }

    val encodeType = requireNotNull(resolveMMKVType(encoder.returnType, encodeDeclaration)) {
      "[${original.logName()}] " +
        "Unsupported type '${encoder.returnType.logName()}' for type converter " +
        "'${encoder.logName()}', consider replacing it with a supported one."
    }

    require(
      encoder.returnType?.resolve()?.isMarkedNullable == false ||
        encodeType == "Bytes" || encodeType == "String" || encodeType == "StringSet"
    ) {
      "[${original.logName()}] " +
        "The return type of the type converter '${encoder.logName()}' " +
        "must not be nullable, consider replacing it with a supported one."
    }

    return TypeConverter(
      decoder = decoder,
      encoder = encoder,
      decoderName = decoder.toMemberName(),
      encoderName = encoder.toMemberName(),
      encodeType = encodeType,
    )
  }

  data class TypeConverter(
    val decoder: KSFunctionDeclaration,
    val encoder: KSFunctionDeclaration,
    val decoderName: MemberName,
    val encoderName: MemberName,
    val encodeType: String,
  ) {
    val decoderProvider: String? = decoder.parentDeclaration?.getProviderName()
    val encoderProvider: String? = encoder.parentDeclaration?.getProviderName()

    private fun KSDeclaration.getProviderName() = this
      .takeIf { (it as? KSClassDeclaration)?.classKind == ClassKind.CLASS }
      ?.simpleName
      ?.asString()
      ?.replaceFirstChar(Char::lowercase)
  }
}
