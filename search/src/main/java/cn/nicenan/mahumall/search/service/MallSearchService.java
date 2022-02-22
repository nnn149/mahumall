package cn.nicenan.mahumall.search.service;

import cn.nicenan.mahumall.search.vo.SearchParam;
import cn.nicenan.mahumall.search.vo.SearchResult;


public interface MallSearchService {
    /**
     * 搜索商品
     * @param searchParam 检索的参数
     * @return 页面包含的所有信息
     */
    SearchResult search(SearchParam searchParam);
}
