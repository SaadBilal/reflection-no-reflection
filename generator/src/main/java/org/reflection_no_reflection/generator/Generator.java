package org.reflection_no_reflection.generator;

import com.squareup.javapoet.JavaFile;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import org.reflection_no_reflection.Class;
import org.reflection_no_reflection.processor.Processor;
import org.reflection_no_reflection.visit.ClassPoolVisitStrategy;

/**
 * An annotation processor sample that demonstrates how to use the RNR annotation processor.
 */
public class Generator extends AbstractProcessor {

    private Processor processor = new Processor();
    private ProcessingEnvironment processingEnv;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        //comma separated list of injected classes
        processor.init(processingEnv);
        processor.setTargetAnnotatedClasses(new HashSet<>(Arrays.asList(javax.inject.Inject.class.getName())));
        System.out.println("RNR Generator created.");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        //TODO
        boolean processed = processor.process(annotations, roundEnv);

        if (!roundEnv.processingOver()) {
            return processed;
        }
        HashSet<Class> annotatedClassSet = new HashSet<>(processor.getAnnotatedClassSet());
        ModuleDumperClassPoolVisitor moduleDumper = new ModuleDumperClassPoolVisitor();
        moduleDumper.getMapAnnotationTypeToClassContainingAnnotation().putAll(processor.getMapAnnotationTypeToClassContainingAnnotation());
        JavaFile rnRModuleJavaFile = createRnRModuleJavaFile(annotatedClassSet, moduleDumper);
        writeJavaFile(rnRModuleJavaFile);
        System.out.println("Dumping all collected data: \n");
        printJavaFile(rnRModuleJavaFile);

        IntrospectorDumperClassPoolVisitor reflectorsDumper = new IntrospectorDumperClassPoolVisitor();
        ClassPoolVisitStrategy visitor = new ClassPoolVisitStrategy();
        visitor.visit(annotatedClassSet, reflectorsDumper);

        for (JavaFile javaFile : reflectorsDumper.getJavaFiles()) {
            writeJavaFile(javaFile);
            System.out.println("Dumping reflector: \n");
        }

        return processed;
    }

    private void printJavaFile(JavaFile javaFile) {
        String buffer = javaFile.toString();
        System.out.println(buffer);
    }

    private void writeJavaFile(JavaFile javaFile) {
        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JavaFile createRnRModuleJavaFile(HashSet<Class> annotatedClassSet, ModuleDumperClassPoolVisitor dumper) {
        ClassPoolVisitStrategy visitor = new ClassPoolVisitStrategy();
        visitor.visit(annotatedClassSet, dumper);

        return dumper.getJavaFile();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return processor.getSupportedAnnotationTypes();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return processor.getSupportedSourceVersion();
    }

    @Override
    public Set<String> getSupportedOptions() {
        return processor.getSupportedOptions();
    }
}