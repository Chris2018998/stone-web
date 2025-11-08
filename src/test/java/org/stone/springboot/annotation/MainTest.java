package org.stone.springboot.annotation;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class MainTest {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();


        mapper.addMixIn(Person.class, UserMixIn.class);


        Person p = new Person();
        p.setName("Chris");
        p.setAge(10);
        p.setStatement(new Object());
        System.out.println(mapper.writeValueAsString(p));

//        Class<Person> personClass = Person.class;
//        Field field = personClass.getDeclaredField("name");
//        field.setAccessible(true);
//
//        Annotation[] annotations = field.getAnnotations();
//        Annotation[] newAnnotations = new Annotation[annotations.length + 1];
//        System.arraycopy(annotations, 0, newAnnotations, 0, annotations.length);
//        newAnnotations[annotations.length] = field.getAnnotation(MyAnnotation.class);
//        System.out.println(field.getAnnotation(MyAnnotation.class));
    }
}
