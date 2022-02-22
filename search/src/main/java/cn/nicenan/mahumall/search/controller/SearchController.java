package cn.nicenan.mahumall.search.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.naming.directory.SearchResult;
import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {
//    @Autowired
//    private MallService masllService;

    @GetMapping("/list.html")
    public String listPage(Model model, HttpServletRequest request){
//SearchParam searchParam,
//        // 获取路径原生的查询属性
//        searchParam.set_queryString(request.getQueryString());
//        // ES中检索到的结果 传递给页面
//        SearchResult result = masllService.search(searchParam);
//        model.addAttribute("result", result);
        return "list";
    }
}
