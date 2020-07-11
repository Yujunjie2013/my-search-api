package cn.com.search.model;

import cn.com.search.annotation.FieldStore;
import lombok.Data;
import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;

import java.util.List;

@Data
public class GeoInfo {
    @FieldStore
    private Long id;
    @FieldStore
    private Long pId;
    @FieldStore
    private String name;
    @FieldStore
    private String code;
    @FieldStore
    private String type;
//    @FieldStore
//    private List<GoodItem> itemList;
//    @FieldStore
//    private byte a;
//
//    private boolean isShow;
}
