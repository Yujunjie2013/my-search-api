package cn.com.search.web;

import cn.com.search.core.Result;
import cn.com.search.core.ResultGenerator;
import cn.com.search.model.GeoInfo;
import cn.com.search.model.GoodItem;
import cn.com.search.service.GoodItemService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by CodeGenerator on 2018/12/21.
 */
@Controller
@RequestMapping("/good/item")
@Slf4j
public class GoodItemController {
    @Resource
    private GoodItemService goodItemService;

    @GetMapping("/index")
    public Result index() {
        goodItemService.index();
        return ResultGenerator.genSuccessResult();
    }

    @GetMapping("/createIndex")
    public Result createGeo() {
        goodItemService.createIndex();
        return ResultGenerator.genSuccessResult();
    }

    @GetMapping("/search/geo")
    public Result listSerchGeo(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size,
                               @RequestParam(required = false) String name) {
        PageHelper.startPage(page, size);
        List<GeoInfo> list = goodItemService.searchGeo(name);
        PageInfo<GeoInfo> pageInfo = new PageInfo<>(list);
        return ResultGenerator.genSuccessResult(pageInfo);
    }


    @GetMapping("/search")
    public Result listSerch(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size,
                            @RequestParam(required = false) String name) {
        PageHelper.startPage(page, size);
        Long start = System.currentTimeMillis();
        List<GoodItem> list = goodItemService.search(name);
        PageInfo pageInfo = new PageInfo(list);
        return ResultGenerator.genSuccessResult(pageInfo);
    }

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size,
                       @RequestParam(required = false) String name) {
        PageHelper.startPage(page, size);
        Long start = System.currentTimeMillis();
        List<GoodItem> list = goodItemService.list(name);
        log.info("like查询所需要的时间为{}ms", (System.currentTimeMillis() - start));
        PageInfo pageInfo = new PageInfo(list);
        return ResultGenerator.genSuccessResult(pageInfo);
    }
}
