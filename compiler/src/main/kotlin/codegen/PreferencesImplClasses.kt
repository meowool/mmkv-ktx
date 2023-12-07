/*
 * Copyright (C) 2023 Meowool <https://github.com/meowool/mmkv-ktx/graphs/contributors>
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
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.meowool.mmkv.ktx.compiler.Names.BuiltInConverters
import com.meowool.mmkv.ktx.compiler.Names.MMKV
import com.meowool.mmkv.ktx.compiler.Names.MutableStateFlow
import com.meowool.mmkv.ktx.compiler.Names.Parcelable
import com.meowool.mmkv.ktx.compiler.Names.Preferences
import com.meowool.mmkv.ktx.compiler.Names.addInvisibleSuppress
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy as by

class PreferencesImplClasses(override val context: Context) : Codegen() {
  override fun generate() = context.preferences.forEach(::generatePreferences)

  private fun generatePreferences(preferences: KSClassDeclaration) {
    val dataClassName = preferences.toClassName()
    val className = context.preferencesImplClassName(preferences)
    val mutableClassName = context.mutableClassName(preferences)
    val mutableImplClassName = context.mutableImplClassName(preferences)
    val annotation = requireNotNull(preferences.findAnnotation(Preferences))
    val id = annotation.findStringArgument("id") ?: preferences.simpleName.asString()

    val mmkvPropertySpec = PropertySpec.builder("mmkv", MMKV)
      .addModifiers(KModifier.PRIVATE)
      .initializer("%T.mmkvWithID(%S)", MMKV, id)
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
        preferences.mapAssignments()?.forEach(::add)
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
      .addStatement("%N?.value = mutable.toImmutable()", statePropertySpec)
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
      val resolvedType = type.resolve()
      val declaration = requireNotNull(resolvedType.findActualDeclaration())
      val primitive = resolveMMKVType(type, declaration)

      mutableImplClassBuilder.addProperty(
        PropertySpec.builder(name, type.toTypeName())
          .addModifiers(KModifier.OVERRIDE)
          .initializer("old.%L", name)
          .setter(
            FunSpec.setterBuilder()
              .addParameter("new", type.toTypeName())
              .addCode(buildCodeBlock {
                addStatement("field = new")
                when {
                  primitive != null && resolvedType.isMarkedNullable -> when (primitive) {
                    "Bool" -> addStatement(
                      "mmkv.encode(%S, %T.encodeNullableBoolean(new))",
                      name, BuiltInConverters
                    )
                    "Bytes", "String", "StringSet" -> addStatement("mmkv.encode(%S, new)", name)
                    else -> addStatement(
                      "mmkv.encode(%S, %T.encodeNullable%L(new))",
                      name, BuiltInConverters, primitive
                    )
                  }

                  primitive != null || declaration.superTypes.contains(Parcelable) ->
                    addStatement("%N.encode(%S, new)", mmkvPropertySpec, name)

                  Modifier.ENUM in declaration.modifiers -> addStatement(
                    when (resolvedType.isMarkedNullable) {
                      true -> "%N.encode(%S, new?.ordinal ?: -1)"
                      false -> "%N.encode(%S, new.ordinal)"
                    },
                    mmkvPropertySpec, name
                  )

                  else -> {
                    val typeConverter = requireNotNull(resolvedType.findTypeConverter(declaration)) {
                      "[${preferences.logName()}] " +
                        "Unsupported type '${type.logName()}' for property '$name', " +
                        "consider replacing it with a supported one or creating a type converter."
                    }
                    addStatement(
                      "%N.encode(%S, new.%M())",
                      mmkvPropertySpec, name, typeConverter.encoderName,
                    )
                  }
                }
              })
              .build()
          )
          .mutable()
          .build()
      )
    }

    val classSpec = TypeSpec.classBuilder(className)
      .addModifiers(KModifier.INTERNAL)
      .addSuperinterface(context.preferencesClassName(preferences))
      .addAnnotation(PublishedApi::class)
      .addProperty(mmkvPropertySpec)
      .addProperty(defaultPropertySpec)
      .addProperty(instancePropertySpec)
      .addProperty(statePropertySpec)
      .addFunction(getFunSpec)
      .addFunction(mutableFunSpec)
      .addFunction(updateFunSpec)
      .addFunction(asStateFlowFunSpec)
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

  private fun KSClassDeclaration.mapAssignments() = primaryConstructor?.parameters?.map {
    val name = requireNotNull(it.name?.asString())
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
          addStatement("val·entries·=·%T.entries", declaration.toClassName())
          addStatement("val·ordinal·=·mmkv.decodeInt(%S, -1)", name)
          addStatement("if·(ordinal == -1)·return@run·default.$name")
          addStatement("entries[ordinal]")
          unindent()
          addStatement("},")
        }
        declaration.superTypes.contains(Parcelable) -> buildCodeBlock {
          addStatement(
            "$name·=·mmkv.decodeParcelable(%S,·%T::class.java)·?:·default.$name,",
            name, declaration.toClassName()
          )
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
            when (typeConverter.encodeType) {
              "Bool" -> {
                addStatement("val·value·=·mmkv.decodeBool(%S,·false)", name)
                addStatement("if·(!value)·return@run·default.$name")
              }
              "Int" -> {
                addStatement("val·value·=·mmkv.decodeInt(%S,·-1)", name)
                addStatement("if·(value == -1)·return@run·default.$name")
              }
              "Long" -> {
                addStatement("val·value·=·mmkv.decodeLong(%S,·-1)", name)
                addStatement("if·(value == -1L)·return@run·default.$name")
              }
              "Float" -> {
                addStatement("val·value·=·mmkv.decodeFloat(%S,·Float.NaN)", name)
                addStatement("if·(value.isNaN())·return@run·default.$name")
              }
              "Double" -> {
                addStatement("val·value·=·mmkv.decodeDouble(%S,·Double.NaN)", name)
                addStatement("if·(value.isNaN())·return@run·default.$name")
              }
              "Bytes" -> {
                addStatement("val·value·=·mmkv.decodeBytes(%S)", name)
                addStatement("if·(value == null)·return@run·default.$name")
              }
              "String" -> {
                addStatement("val·value·=·mmkv.decodeString(%S)", name)
                addStatement("if·(value == null)·return@run·default.$name")
              }
              "StringSet" -> {
                addStatement("val·value·=·mmkv.decodeStringSet(%S)", name)
                addStatement("if·(value == null)·return@run·default.$name")
              }
              else -> error(
                "[${typeConverter.encoder.logName()}] " +
                  "Unsupported type '${typeConverter.encodeType}' for property '$name', " +
                  "consider replacing it with a supported one."
              )
            }
            addStatement("value.%M()", typeConverter.decoderName)
            unindent()
            addStatement("},")
          }
        }
      }
      else -> buildCodeBlock {
        when {
          primitive == "Bytes" || primitive == "String" || primitive == "StringSet" ->
            addStatement("$name·=·mmkv.decode%L(%S)·?:·default.$name,", primitive, name)

          resolvedType.isMarkedNullable -> when (primitive) {
            "Bool" -> addStatement(
              "$name·=·%T.decodeNullableBoolean(mmkv.decodeInt(%S,·-1))·?:·default.$name,",
              BuiltInConverters, name
            )
            "Int" -> addStatement(
              "$name·=·%T.decodeNullableInt(mmkv.decodeLong(%S,·Long.MAX_VALUE))·?:·default.$name,",
              BuiltInConverters, name
            )
            else -> addStatement(
              "$name·=·%T.decodeNullable%L(mmkv.decodeBytes(%S))·?:·default.$name,",
              BuiltInConverters, primitive, name
            )
          }

          else -> addStatement("$name·=·mmkv.decode$primitive(%S,·default.$name),", name)
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

    require(encoder.returnType?.resolve()?.isMarkedNullable == false) {
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
  )
}
