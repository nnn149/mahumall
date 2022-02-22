package cn.nicenan.mahumall.search.controller;

import cn.nicenan.mahumall.search.service.MallSearchService;
import cn.nicenan.mahumall.search.vo.SearchParam;
import cn.nicenan.mahumall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {
    @Autowired
    private MallSearchService mallSearchService;

    @GetMapping("/list.html")
    public String listPage(SearchParam searchParam, Model model, HttpServletRequest request) {
        searchParam.set_queryString(request.getQueryString());
        // ES中检索到的结果 传递给页面
        SearchResult result = mallSearchService.search(searchParam);
        model.addAttribute("result", result);
        return "list";
    }
}
