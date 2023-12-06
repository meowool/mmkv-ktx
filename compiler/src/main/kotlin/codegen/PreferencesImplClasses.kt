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
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.meowool.mmkv.ktx.compiler.Names.MMKV
import com.meowool.mmkv.ktx.compiler.Names.MutableStateFlow
import com.meowool.mmkv.ktx.compiler.Names.Parcelable
import com.meowool.mmkv.ktx.compiler.Names.Preferences
import com.meowool.mmkv.ktx.compiler.Names.addInternalImport
import com.meowool.mmkv.ktx.compiler.Names.addInvisibleSuppress
import com.meowool.mmkv.ktx.compiler.codegen.Codegen.Context
import com.squareup.kotlinpoet.ANY
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
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy as by

class PreferencesImplClasses(override val context: Context) : Codegen {
  override fun generate() = context.preferences.forEach(::generatePreferences)

  private fun generatePreferences(preferences: KSClassDeclaration) {
    val dataClassName = preferences.toClassName()
    val className = context.preferencesImplClassName(preferences)
    val mutableClassName = context.mutableClassName(preferences)
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

    val instancePropertySpec = PropertySpec.builder("_instance", dataClassName)
      .mutable()
      .addModifiers(KModifier.PRIVATE)
      .addAnnotation(Volatile::class)
      .build()

    val statePropertySpec = PropertySpec.builder("_state", MutableStateFlow.by(dataClassName))
      .mutable()
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
        addStatement(").also { %N = it }", instancePropertySpec)
      })
      .build()

    val mutableFunSpec = FunSpec.builder("mutable")
      .addModifiers(KModifier.OVERRIDE)
      .returns(mutableClassName)
      .addStatement("return %T()", mutableClassName)
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
          "%T(get()).also { %N = it }",
          MutableStateFlow.by(dataClassName), statePropertySpec
        )
      })
      .build()

    val mutableImplClassName = context.mutableImplClassName(preferences)
    val mutableImplClassBuilder = TypeSpec.classBuilder(mutableImplClassName)
      .addModifiers(KModifier.INNER)
      .addSuperinterface(mutableClassName)
      .addFunction(
        FunSpec.builder("toImmutable")
          .addModifiers(KModifier.OVERRIDE)
          .returns(dataClassName)
          .beginControlFlow("return get().also { old ->")
          .addCode(buildCodeBlock {
            addStatement("%T(", dataClassName)
            indent()
            preferences.primaryConstructor?.parameters?.forEach { parameter ->
              val name = requireNotNull(parameter.name?.asString())
              val new = "new${name.uppercaseFirstChar()}"
              addStatement(
                "%L = %N.takeIfChanged() ?: old.%L,",
                name, new, name
              )
            }
            unindent()
            addStatement(")")
          })
          .endControlFlow()
          .build()
      )

    preferences.primaryConstructor?.parameters?.forEach { parameter ->
      val name = requireNotNull(parameter.name?.asString())
      val new = "new${name.uppercaseFirstChar()}"
      val type = parameter.type
      val declaration = requireNotNull(type.findActualDeclaration())
      val primitive = resolveMMKVType(type, declaration)

      val newPropertySpec = PropertySpec.builder(new, ANY.copy(nullable = true))
        .mutable()
        .addModifiers(KModifier.PRIVATE)
        .initializer("UNCHANGED")
        .build()

      val overridePropertySpec = PropertySpec.builder(name, type.toTypeName())
        .getter(
          FunSpec.getterBuilder()
            .addStatement("throw %T(%S)", UnsupportedOperationException::class, "WRITE_ONLY")
            .build()
        )
        .setter(
          FunSpec.setterBuilder()
            .addParameter("value", type.toTypeName())
            .addCode(buildCodeBlock {
              when {
                primitive != null || declaration.superTypes.contains(Parcelable) ->
                  addStatement("%N.encode(%S, value)", mmkvPropertySpec, name)

                Modifier.ENUM in declaration.modifiers ->
                  addStatement("%N.encode(%S, value.ordinal)", mmkvPropertySpec, name)

                else -> {
                  val typeConverter = requireNotNull(type.findTypeConverter(declaration)) {
                    "[${preferences.fullName()}] " +
                      "Unsupported type '$type' for property '$name', " +
                      "consider replacing it with a supported one or creating a type converter."
                  }
                  addStatement(
                    "%N.encode(%S, value.%M())",
                    mmkvPropertySpec, name, typeConverter.encoderName,
                  )
                }
              }
              addStatement("%N = value", newPropertySpec)
            })
            .build()
        )
        .mutable()
        .build()

      mutableImplClassBuilder.addProperty(newPropertySpec)
      mutableImplClassBuilder.addProperty(overridePropertySpec)
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
      .addInternalImport()
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
    val declaration = requireNotNull(it.type.findActualDeclaration()) {
      "[${this.fullName()}] " +
        "Cannot find actual declaration of the type ${it.type} for property '$name', " +
        "this may be due to the type is not supported, " +
        "consider replacing it with a supported one."
    }
    when (val primitive = resolveMMKVType(type, declaration)) {
      null -> when {
        Modifier.ENUM in declaration.modifiers -> buildCodeBlock {
          beginControlFlow("$name = run")
          addStatement("val·entries·=·${declaration.simpleName.asString()}.entries")
          addStatement("val·ordinal·=·mmkv.decodeInt(%S, -1)", name)
          addStatement("if·(ordinal == -1)·return@run·default.$name")
          addStatement("entries[ordinal]")
          endControlFlow()
        }
        declaration.superTypes.contains(Parcelable) -> CodeBlock.of(
          "$name·=·mmkv.decodeParcelable(%S,·%T::class.java,·default.$name),",
          name, declaration.toClassName()
        )
        else -> {
          val typeConverter = requireNotNull(type.findTypeConverter(this)) {
            "[${(this.qualifiedName ?: this.simpleName).asString()}] " +
              "Unsupported type '$type' for property '$name', " +
              "consider replacing it with a supported one or creating a type converter."
          }

          buildCodeBlock {
            beginControlFlow("$name = run")
            when (typeConverter.decodeType) {
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
              else -> error("Unsupported type '${typeConverter.decodeType}'")
            }
            addStatement("value.%M()", typeConverter.decoderName)
            endControlFlow()
          }
        }
      }
      else -> CodeBlock.of("$name·=·mmkv.decode$primitive(%S,·default.$name),", name)
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

  private fun Any.findTypeConverter(original: KSDeclaration): TypeConverter? {
    val typeConverters = context.typeConverters.flatMap(KSClassDeclaration::getDeclaredFunctions)
    val encoder = typeConverters.filter(KSDeclaration::isPublic).firstOrNull {
      it.parameters.size == 1 && it.extensionReceiver.matches(this)
    }
    val decoder = typeConverters.filter(KSDeclaration::isPublic).firstOrNull {
      it.parameters.size == 1 &&
        it.returnType.matches(this) &&
        it.extensionReceiver.matches(encoder?.returnType)
    }
    if (encoder == null || decoder == null) return null

    val decodeDeclaration = requireNotNull(decoder.returnType?.findActualDeclaration()) {
      "[${original.fullName()}] " +
        "Cannot find actual declaration of the type ${decoder.returnType} " +
        "for type converter '${decoder.fullName()}', " +
        "this may be due to the type is not supported, " +
        "consider replacing it with a supported one."
    }

    val encodeDeclaration = requireNotNull(encoder.extensionReceiver?.findActualDeclaration()) {
      "[${original.fullName()}] " +
        "Cannot find actual declaration of the type ${encoder.extensionReceiver} " +
        "for type converter '${encoder.fullName()}', " +
        "this may be due to the type is not supported, " +
        "consider replacing it with a supported one."
    }

    val decodeType = requireNotNull(resolveMMKVType(decoder.returnType, decodeDeclaration)) {
      "[${original.fullName()}] " +
        "Unsupported type '${decoder.returnType}' for type converter " +
        "'${decoder.fullName()}', consider replacing it with a supported one."
    }

    val encodeType = requireNotNull(resolveMMKVType(encoder.extensionReceiver, encodeDeclaration)) {
      "[${original.fullName()}] " +
        "Unsupported type '${encoder.extensionReceiver}' for type converter " +
        "'${encoder.fullName()}', consider replacing it with a supported one."
    }

    return TypeConverter(
      decoder = decoder,
      encoder = encoder,
      decoderName = decoder.toMemberName(),
      encoderName = encoder.toMemberName(),
      decodeType = decodeType,
      encodeType = encodeType,
    )
  }

  data class TypeConverter(
    val decoder: KSFunctionDeclaration,
    val encoder: KSFunctionDeclaration,
    val decoderName: MemberName,
    val encoderName: MemberName,
    val decodeType: String,
    val encodeType: String,
  )
}
