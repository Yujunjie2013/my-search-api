package cn.com.search.service.impl;

import cn.com.search.core.AbstractService;
import cn.com.search.dao.GoodItemMapper;
import cn.com.search.model.GeoInfo;
import cn.com.search.model.GoodItem;
import cn.com.search.service.GoodItemService;
import cn.com.search.util.LuceneUtils;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wltea.analyzer.core.IKAnalyzer5x;
import tk.mybatis.mapper.entity.Condition;
import tk.mybatis.mapper.entity.Example.Criteria;

import javax.annotation.Resource;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by CodeGenerator on 2018/12/21.
 */
@Service
@Transactional
@Slf4j
public class GoodItemServiceImpl extends AbstractService<GoodItem> implements GoodItemService {

    // 写索引实例
    private IndexWriter writer;

    private String indexDir = "/Users/yujunjie/tmp/lucense_db";

    @Resource
    private GoodItemMapper bbgDtpMomItemMapper;

    @Override
    public List<GoodItem> list(String name) {
        Condition condition = new Condition(GoodItem.class);
        Criteria criteria = condition.createCriteria();
        criteria.andLike("itemDesc", "%" + name + "%");
        List<GoodItem> goodItems = super.findByCondition(condition);
        return goodItems;
    }

    @Override
    public List<GoodItem> search(String name) {
        List<GoodItem> goods = new ArrayList<>();
        try {
            Directory dir = FSDirectory.open(Paths.get(indexDir));
            // 创建索引读取器
            IndexReader reader = DirectoryReader.open(dir);
            // 创建索引查询器
            IndexSearcher is = new IndexSearcher(reader);
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            // 标准分词器
            Analyzer analyzer = new IKAnalyzer5x();    //建立的时候建两套  但是搜索的时候只有一套
            QueryParser queryParser = new MultiFieldQueryParser(new String[]{"content", "title"}, analyzer);
            QueryParser parser = new QueryParser("itemDesc", analyzer);
            parser.setDefaultOperator(Operator.AND);        //AND 或者OR

            Query query = parser.parse(name);
            builder.add(query, Occur.MUST);

            long start = System.currentTimeMillis();
            System.out.println(query.toString());
            TopDocs hits = is.search(builder.build(), 10);
            long end = System.currentTimeMillis();
            System.out.println(
                    "匹配 " + builder.toString() + " ，总共花费" + (end - start) + "毫秒" + "查询到" + hits.totalHits + "个记录");
            for (ScoreDoc scoreDoc : hits.scoreDocs) {
                Document doc = is.doc(scoreDoc.doc);
                GoodItem good = new GoodItem();
                good.setItemDesc(doc.get("itemDesc"));
                good.setId(Long.valueOf(doc.get("id")));
                goods.add(good);
            }
            reader.close();
        } catch (Exception e) {
            log.error("索引查询失败", e);
        }
        return goods;
    }

    @Override
    public void index() {
        try {
            Directory dir = FSDirectory.open(Paths.get(indexDir));
            // 标准分词器
            Analyzer analyzer = new IKAnalyzer5x();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            writer = new IndexWriter(dir, iwc);
            int total = bbgDtpMomItemMapper.selectCount(null);
            int size = 100;
            int page = total / size;
            if (page > 100) page = 100;
            if (total % size != 0)
                page++;
            log.info("goods数据库中记录为{}，按size={}，page={}", total, size, page);
            for (int i = 1; i <= page; i++) {
                log.info("生成page={}的索引", page);
                PageHelper.startPage(i, size);
                List<GoodItem> goods = bbgDtpMomItemMapper.selectAll();
                List<Document> documents = getDocument(goods);
                writer.addDocuments(documents);        //写入索引
                PageHelper.clearPage();
            }
            log.info("索引生成成功");
            writer.close();
        } catch (Exception e) {
            log.error("索引构建失败", e);
        }
    }

    private List<Document> getDocument(List<GoodItem> goods) throws Exception {
        List<Document> docs = new ArrayList<>();
        for (GoodItem good : goods) {
            Document doc = new Document();
            doc.add(new LongField("id", good.getId(), Field.Store.YES));        //Long 是不支持分词的
            doc.add(new TextField("itemDesc", good.getItemDesc(), Field.Store.YES));            //才支持分词 yes表示字段是存储的
            if (good.getMfgRecRetail() != null)
                doc.add(new DoubleField("price", good.getMfgRecRetail(), Field.Store.YES));
            docs.add(doc);
        }
        return docs;
    }


    @Override
    public void update(Long id) {
        GeoInfo geoInfo = bbgDtpMomItemMapper.findGeoInfoById(id);
        try {
            LuceneUtils.update("id", String.valueOf(id), geoInfo);
        } catch (Exception e) {
            log.error("更新信息错误:", e);
        }
    }

    @Override
    public void delete(String field, String value) {
        try {
            LuceneUtils.delete(field, value);
        } catch (Exception e) {
            log.error("删除异常field:" + field, e);
        }
    }

    @Override
    public void deleteAll() {
        try {
            LuceneUtils.deleteAll();
        } catch (Exception e) {
            log.error("删除全部异常:", e);
        }
    }
    @Override
    public void add() {
        GeoInfo geoInfo = new GeoInfo();
        geoInfo.setName("测试添加");
        geoInfo.setId(new Random().nextLong());
        try {
            LuceneUtils.add(geoInfo);
        } catch (Exception e) {
            log.error("添加记录", e);
        }
    }

    @Override
    public void createIndex() {
        List<GeoInfo> geoInfos = bbgDtpMomItemMapper.selectAllGeo();
        try {
            LuceneUtils.addAll(geoInfos);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("创建索引错误:", e);
        }
    }

    @Override
    public List<GeoInfo> searchGeo(String value) {
        try {
            return LuceneUtils.search("name", value, 100, GeoInfo.class);
        } catch (Exception e) {
            log.error("搜索异常:", e);
        }
        return null;
    }
}
