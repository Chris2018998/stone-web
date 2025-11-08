package org.stone.springboot.annotation;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;
import org.mockito.internal.matchers.NotNull;

import java.lang.reflect.Field;

public class DynamicAnnotationDemo {
    public static void main(String[] args) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.get("org.stone.springboot.annotation.MyClass");
        CtField ctField = ctClass.getDeclaredField("myField");
        ConstPool constPool = ctClass.getClassFile().getConstPool();


        // 创建注解对象
        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        Annotation annotation = new Annotation("NotNull",constPool);
        annotation.addMemberValue("message", new StringMemberValue("姓名不能为空", constPool));
        attr.addAnnotation(annotation);

        // 将注解添加到字段
        ctField.getFieldInfo().addAttribute(attr);

//        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
//
//        // 添加@Deprecated注解
//        Annotation annot = new Annotation("java.lang.Deprecated", constPool);
//        attr.addAnnotation(annot);
//

        // 加载并测试动态类
        Class<?> dynamicClass = ctClass.toClass();
//        Field field = dynamicClass.getDeclaredField("name");
//        NotNull notNull = field.getAnnotation(NotNull.class);
//        System.out.println("动态添加的注解值: " + notNull.message());
    }
}
