/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package org.graalvm.compiler.replacements.verifier;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

public class VerifierAnnotationProcessor extends AbstractProcessor {

    private List<AbstractVerifier> verifiers;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            PluginGenerator generator = new PluginGenerator();
            for (AbstractVerifier verifier : getVerifiers()) {
                Class<? extends Annotation> annotationClass = verifier.getAnnotationClass();
                for (Element e : roundEnv.getElementsAnnotatedWith(annotationClass)) {
                    AnnotationMirror annotationMirror = findAnnotationMirror(processingEnv, e.getAnnotationMirrors(), annotationClass);
                    if (annotationMirror == null) {
                        assert false : "Annotation mirror always expected.";
                        continue;
                    }
                    verifier.verify(e, annotationMirror, generator);
                }
            }

            generator.generateAll(processingEnv);
        }
        return false;
    }

    public static AnnotationMirror findAnnotationMirror(ProcessingEnvironment processingEnv, List<? extends AnnotationMirror> mirrors, Class<?> annotationClass) {
        TypeElement expectedAnnotationType = processingEnv.getElementUtils().getTypeElement(annotationClass.getCanonicalName());
        for (AnnotationMirror mirror : mirrors) {
            DeclaredType annotationType = mirror.getAnnotationType();
            TypeElement actualAnnotationType = (TypeElement) annotationType.asElement();
            if (actualAnnotationType.equals(expectedAnnotationType)) {
                return mirror;
            }
        }
        return null;
    }

    public List<AbstractVerifier> getVerifiers() {
        /*
         * Initialized lazily to fail(CNE) when the processor is invoked and not when it is created.
         */
        if (verifiers == null) {
            assert this.processingEnv != null : "ProcessingEnv must be initialized before calling getVerifiers.";
            verifiers = new ArrayList<>();
            verifiers.add(new ClassSubstitutionVerifier(this.processingEnv));
            verifiers.add(new MethodSubstitutionVerifier(this.processingEnv));
            verifiers.add(new NodeIntrinsicVerifier(this.processingEnv));
            verifiers.add(new FoldVerifier(this.processingEnv));
        }
        return verifiers;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotationTypes = new HashSet<>();
        for (AbstractVerifier verifier : getVerifiers()) {
            annotationTypes.add(verifier.getAnnotationClass().getCanonicalName());
        }
        return annotationTypes;
    }

}
