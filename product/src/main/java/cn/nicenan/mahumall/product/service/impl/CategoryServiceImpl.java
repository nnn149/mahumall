package cn.nicenan.mahumall.product.service.impl;

import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.common.utils.Query;
import cn.nicenan.mahumall.product.dao.CategoryDao;
import cn.nicenan.mahumall.product.entity.CategoryEntity;
import cn.nicenan.mahumall.product.service.CategoryBrandRelationService;
import cn.nicenan.mahumall.product.service.CategoryService;
import cn.nicenan.mahumall.product.vo.Catelog2Vo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1.查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        //2.组装成父子的树形结构
        //2.1找出所有1级分类
        List<CategoryEntity> level1Menu = entities.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                .peek(menu -> menu.setChildren(getChildrens(menu, entities)))
                .sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort())))
                .collect(Collectors.toList());
        return level1Menu;
    }

    @Override
    public void removeMenuByIds(List<Long> singletonList) {
        //todo 检查当前删除的菜单是否被引用

        baseMapper.deleteBatchIds(singletonList);
    }

    /**
     * 找到完整路径 父，子，孙
     *
     * @param catelogId
     * @return
     */
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> path = findParentPath(catelogId, new ArrayList<>());
        Collections.reverse(path);
        return (path.toArray(new Long[path.size()]));
    }

    /**
     * 级联更新所有关联的数据
     *
     * @param category
     */
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }

    @Override
    public Map<Long, List<Catelog2Vo>> getCatalogJson() {
        //1.查出所有一级分类
        List<CategoryEntity> level1Categorys = getLevel1Categorys();
        //2.封装数据
        Map<Long, List<Catelog2Vo>> listMap = level1Categorys.stream().collect(Collectors.toMap(CategoryEntity::getCatId, level1 -> {
            //2.1查出所有二级分类
            List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", level1.getCatId()));
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities.size() > 0) {
                catelog2Vos = categoryEntities.stream().map(level2 -> {
                    Catelog2Vo catelog2Vo =
                            new Catelog2Vo(level2.getCatId().toString(),
                                    level2.getName(),
                                    level2.getParentCid().toString(),
                                    null
                            );
                    //2.2查出所有三级分类
                    List<CategoryEntity> level3Categorys = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", level2.getCatId()));
                    if (level3Categorys != null && level3Categorys.size() > 0) {
                        List<Catelog2Vo.Catalog3Vo> catalog3Vos = level3Categorys.stream().map(level3 -> {
                            Catelog2Vo.Catalog3Vo catalog3Vo = new Catelog2Vo.Catalog3Vo(level3.getCatId().toString(), level3.getName(), level3.getParentCid().toString());
                            return catalog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(catalog3Vos);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        return listMap;
    }

    private List<Long> findParentPath(Long catelogId, List<Long> path) {
        path.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), path);
        }
        return path;
    }

    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid().equals(root.getCatId()))
                //找到子菜单
                .peek(categoryEntity -> categoryEntity.setChildren(getChildrens(categoryEntity, all)))
                .sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort())))
                .collect(Collectors.toList());
        return children;
    }
}
