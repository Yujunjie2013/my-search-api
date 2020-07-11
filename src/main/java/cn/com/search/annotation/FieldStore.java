package cn.com.search.annotation;


import org.apache.lucene.document.Field;

import java.lang.annotation.*;

/**
 * 在需要存储的字段上使用该注解
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FieldStore {
    //默认存储，如果不需要使用Field.Store.NO
    Field.Store store() default Field.Store.YES;
}

