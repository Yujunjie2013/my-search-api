package cn.com.search.util;

import cn.com.search.annotation.FieldStore;
import cn.com.search.model.GeoInfo;
import com.github.pagehelper.Page;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.util.Assert;
import org.wltea.analyzer.core.IKAnalyzer5x;

import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Lucense工具类
 */
public class LuceneUtils {
    private static Directory directory;
    private static Analyzer analyzer;
    private static final String LUCENE_DIRECTORY = "/Users/yujunjie/tmp/lucense_db";

    static {
        try {
            directory = FSDirectory.open(Paths.get(LUCENE_DIRECTORY));
            analyzer = new IKAnalyzer5x();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //不让外部new当前帮助类的对象
    private LuceneUtils() {
    }


    public static IndexWriterConfig indexWriterConfig() {
        return new IndexWriterConfig(analyzer);
    }

    /**
     * 添加记录
     */
    public static <T> void add(T t) throws Exception {
        Assert.notNull(t, "添加的对象不能为null");
        Document document = javabean2document(t);
        try (IndexWriter indexWriter = new IndexWriter(getDirectory(), indexWriterConfig())) {
            indexWriter.addDocument(document);
        }
    }

    /**
     * 批量添加
     */
    public static <T> void addAll(List<T> list) throws Exception {
        Assert.notNull(list, "更新的集合不能为null");
        try (IndexWriter indexWriter = new IndexWriter(getDirectory(), indexWriterConfig())) {
            ArrayList<Document> documents = new ArrayList<>();
            for (T t : list) {
                Document doc = javabean2document(t);
                documents.add(doc);
            }
            indexWriter.addDocuments(documents);
        }
    }

    /**
     * 根据条件更新
     */
    public static <T> void update(String field, String value, T t) throws Exception {
        Assert.notNull(field, "更新的field不能为null");
        Assert.notNull(value, "更新的value不能为null");
        Assert.notNull(t, "更新的对象不能为null");
        Document document = javabean2document(t);
        try (IndexWriter indexWriter = new IndexWriter(getDirectory(), indexWriterConfig())) {
            indexWriter.updateDocument(new Term(field, value), document);
        }
    }

    /**
     * 根据条件删除
     */
    public static void delete(String field, String value) throws Exception {
        Assert.notNull(field, "删除条件的field不能为null");
        Assert.notNull(value, "删除条件的value不能为null");
        try (IndexWriter indexWriter = new IndexWriter(getDirectory(), indexWriterConfig())) {
            indexWriter.deleteDocuments(new Term(field, value));
        }
    }

    /**
     * 删除所有记录
     */
    public static void deleteAll() throws Exception {
        try (IndexWriter indexWriter = new IndexWriter(getDirectory(), indexWriterConfig())) {
            indexWriter.deleteAll();
        }
    }

    /**
     * 根据关键字进行搜索
     */
    public static <T> List<T> search(String field, String keyword, int topN, Class<T> clazz) throws Exception {
        List<T> list = new ArrayList<>();
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        QueryParser queryParser = new QueryParser(field, getAnalyzer());
        queryParser.setDefaultOperator(QueryParser.Operator.AND);        //AND 或者OR
        Query query = queryParser.parse(keyword);
        builder.add(query, BooleanClause.Occur.MUST);
        try (DirectoryReader reader = DirectoryReader.open(getDirectory())) {
            IndexSearcher indexSearcher = new IndexSearcher(reader);
            TopDocs topDocs = indexSearcher.search(builder.build(), Math.min(topN, 5000));
            for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[i];
                int docIndex = scoreDoc.doc;
                System.out.println("文档索引号" + docIndex + "，文档得分：" + scoreDoc.score);
                Document document = indexSearcher.doc(docIndex);
                T entity = document2javabean(document, clazz);
                list.add(entity);
            }
        }
        return list;
    }

    /**
     * 分页查询
     */
    public static <T> Page<T> pagination(int page, int pageSize, String field, String keyword, Class<T> clazz) throws Exception {
        if (page <= 0) {
            page = 1;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        QueryParser queryParser = new QueryParser(field, getAnalyzer());
        Query query = queryParser.parse(keyword);
        try (DirectoryReader open = DirectoryReader.open(getDirectory())) {
            IndexSearcher indexSearcher = new IndexSearcher(open);
            //lucene的分页其实是将数据全部查出来然后再分页，所以当数据量特别大时可能会撑爆内存，故这里需要做一个限制，建议不要超过5000条,根据实际需要调整
            TopDocs topDocs = indexSearcher.search(query, Math.min(page * pageSize, 5000));
            int totalHits = topDocs.totalHits;
            int quotient = totalHits / pageSize;
            int remainder = totalHits % pageSize;
            //计算分出的实际总页数
            int totalPages = remainder == 0 ? quotient : quotient + 1;
            int startIndex = (page - 1) * pageSize;
            int stopIndex = Math.min(startIndex + pageSize, totalHits);
            Page<T> tPage = new Page<>();
            for (int i = startIndex; i < stopIndex; i++) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[i];
                int docIndex = scoreDoc.doc;
                Document document = indexSearcher.doc(docIndex);
                T t = document2javabean(document, clazz);
                tPage.add(t);
            }
            //总数量
            tPage.setTotal(totalHits);
            //总页数
            tPage.setPages(totalPages);
            //当前页
            tPage.setPageNum(page);
            //页面大小
            tPage.setPageSize(pageSize);
            return tPage;
        }
    }

    //将JavaBean转成Document对象
    public static Document javabean2document(Object obj) throws Exception {
        //创建Document对象
        Document document = new Document();
        Class<?> clazz = obj.getClass();
        java.lang.reflect.Field[] reflectFields = clazz.getDeclaredFields();
        //迭代
        for (java.lang.reflect.Field reflectField : reflectFields) {
            //反射,暴力访问
            reflectField.setAccessible(true);
            Object value = reflectField.get(obj);
            if (value == null) {
                continue;
            }
            boolean annotationPresent = reflectField.isAnnotationPresent(FieldStore.class);
            if (annotationPresent) {
                FieldStore annotation = reflectField.getAnnotation(FieldStore.class);
                Field.Store store = annotation.store();
                Field field = getField(reflectField, value, store);
                if (field == null) continue;
                document.add(field);
            }
        }
        //返回document对象
        return document;
    }

    private static Field getField(java.lang.reflect.Field reflectField, Object value, Field.Store store) {
        //获取字段名
        String name = reflectField.getName();
        if (name.equals("serialVersionUID")) {
            return null;
        }
        Class<?> aClass = reflectField.getType();
        Field field;
        //加入到Document对象中去，这时javabean的属性与document对象的属性相同
        if (aClass.isAssignableFrom(String.class)) {
            //获取字段值
            field = new TextField(name, value.toString(), store);
        } else if (aClass.isAssignableFrom(Integer.class) || aClass.isAssignableFrom(int.class)) {
            field = new IntField(name, Integer.parseInt(value.toString()), store);
        } else if (aClass.isAssignableFrom(Long.class) || aClass.isAssignableFrom(long.class)) {
            field = new LongField(name, Long.parseLong(value.toString()), store);
        } else if (aClass.isAssignableFrom(Double.class) || aClass.isAssignableFrom(double.class)) {
            field = new DoubleField(name, Double.parseDouble(value.toString()), store);
        } else if (aClass.isAssignableFrom(Float.class) || aClass.isAssignableFrom(float.class)) {
            field = new FloatField(name, Float.parseFloat(value.toString()), store);
        } else if (aClass.isAssignableFrom(Byte.class) || aClass.isAssignableFrom(byte.class)) {
            field = new Field(name, value.toString(), store, Field.Index.ANALYZED);
        } else {
//                    field = new Field(name, value.toString(), store, Field.Index.ANALYZED);
            return null;
        }
        return field;
    }

    //将Document对象转换成JavaBean对象
    public static <T> T document2javabean(Document document, Class<T> clazz) throws Exception {
        T instance = clazz.newInstance();
        java.lang.reflect.Field[] reflectFields = clazz.getDeclaredFields();
        Method[] methods = clazz.getMethods();
        HashMap<String, Method> stringMethodHashMap = new HashMap<>();
        for (Method method : methods) {
            stringMethodHashMap.put(method.getName(), method);
        }
        for (java.lang.reflect.Field reflectField : reflectFields) {

            reflectField.setAccessible(true);
            String name = reflectField.getName();
            //把序列化id筛选掉
            if (name.equals("serialVersionUID")) {
                continue;
            }
            Class<?> type = clazz.getDeclaredField(name).getType();
            // 首字母大写
            String replace = name.substring(0, 1).toUpperCase()
                    + name.substring(1);
            //获得setter方法
            if (!stringMethodHashMap.containsKey("set" + replace)) {
                System.out.println("不包含该方法:set" + replace);
                continue;
            }


            Method setMethod = clazz.getMethod("set" + replace, type);
            String str = document.get(name);

            if (str != null && !"".equals(str)) {
                // ---判断读取数据的类型
                if (type.isAssignableFrom(String.class)) {
                    setMethod.invoke(instance, str);
                } else if (type.isAssignableFrom(int.class)
                        || type.isAssignableFrom(Integer.class)) {
                    setMethod.invoke(instance, Integer.parseInt(str));
                } else if (type.isAssignableFrom(Double.class)
                        || type.isAssignableFrom(double.class)) {
                    setMethod.invoke(instance, Double.parseDouble(str));
                } else if (type.isAssignableFrom(Boolean.class)
                        || type.isAssignableFrom(boolean.class)) {
                    setMethod.invoke(instance, Boolean.parseBoolean(str));
                } else if (type.isAssignableFrom(Long.class)
                        || type.isAssignableFrom(long.class)) {
                    setMethod.invoke(instance, Long.parseLong(str));
                } else if (type.isAssignableFrom(Float.class)
                        || type.isAssignableFrom(float.class)) {
                    setMethod.invoke(instance, Float.parseFloat(str));
                }
            }
        }
        return instance;
    }

    public static Directory getDirectory() {
        return directory;
    }

    public static void setDirectory(Directory directory) {
        LuceneUtils.directory = directory;
    }


    public static Analyzer getAnalyzer() {
        return analyzer;
    }

    public static void setAnalyzer(Analyzer analyzer) {
        LuceneUtils.analyzer = analyzer;
    }

    public static void main(String[] args) throws Exception {
        GeoInfo geoInfo = new GeoInfo();
//        ArrayList<GoodItem> objects = new ArrayList<>();
//        GoodItem goodItem = new GoodItem();
//        goodItem.setId(1000L);
//        goodItem.setItemDesc("好空调格力造");
//        objects.add(goodItem);
        geoInfo.setId(1L);
        geoInfo.setCode("86");
        geoInfo.setName("河南");
//        geoInfo.setItemList(objects);
//        geoInfo.setShow(true);
//        geoInfo.setA((byte) 12);
        java.lang.reflect.Field[] reflectFields = geoInfo.getClass().getDeclaredFields();
        //迭代
        for (java.lang.reflect.Field reflectField : reflectFields) {
            reflectField.setAccessible(true);
            Object value = reflectField.get(geoInfo);
            if (value == null) {
                continue;
            }
            boolean annotationPresent = reflectField.isAnnotationPresent(FieldStore.class);
            if (annotationPresent) {
                FieldStore annotation = reflectField.getAnnotation(FieldStore.class);
                Field.Store store = annotation.store();
                Field field = getField(reflectField, value, store);
                if (field != null) {
                    System.out.println(field);
                }
            }
        }
    }

}
