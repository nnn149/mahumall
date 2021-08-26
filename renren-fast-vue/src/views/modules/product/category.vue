<template>
  <div>
    <el-tree :data="menus"
             :props="defaultProps"
             :expand-on-click-node="false"
             show-checkbox
             node-key="catId"
             :default-expanded-keys="expandedKey">

      <span class="custom-tree-node" slot-scope="{ node, data }">
        <span>{{ node.label }}</span>
        <span>
          <el-button
            v-if="node.level <=2"
            type="text"
            size="mini"
            @click="() => append(data)"
          >添加</el-button>
          <el-button type="text" size="mini" @click="edit(data)">修改</el-button>
          <el-button
            v-if="node.childNodes.length===0"
            type="text"
            size="mini"
            @click="() => remove(node, data)"
          >删除</el-button>
        </span>
      </span>
    </el-tree>
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="30%"
      :close-on-click-modal="false"
    >
      <el-form :model="category">
        <el-form-item label="分类名称">
          <el-input v-model="category.name" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="category.icon" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="计量单位">
          <el-input v-model="category.productUnit" autocomplete="off"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitData">确 定</el-button>
      </span>
    </el-dialog>
  </div>


</template>
<script>
export default {
  data () {
    return {
      dialogTitle: '添加',
      dialogType: '',
      category: {
        name: '',
        parentCid: 0,
        catLevel: 0,
        showStatus: 1,
        sort: 0,
        productUnit: '',
        icon: '',
        catId: null
      },
      dialogVisible: false,
      menus: [],
      expandedKey: [],
      defaultProps: {
        children: 'children',
        label: 'name'
      }
    }
  },
  activated () {
    this.getMenus()
  },
  methods: {

    getMenus () {
      this.$http({
        url: this.$http.adornUrl('/product/category/list/tree'),
        method: 'get'
      }).then(({data}) => {
        this.menus = data.data
      })
    },
    append (data) {
      this.dialogType = 'add'
      this.dialogVisible = true
      this.dialogTitle = '添加'
      this.category.catId = null
      this.category.parentCid = data.catId
      this.category.catLevel = data.catLevel * 1 + 1
      this.category.name = ''
      this.category.showStatus = 1
      this.category.sort = 0
      this.category.productUnit = ''
      this.category.icon = ''
    },
    // 添加三级分类
    addCategory () {
      this.$http({
        url: this.$http.adornUrl('/product/category/save'),
        method: 'post',
        data: this.$http.adornData(this.category, false)
      }).then(({data}) => {
        this.$message({
          message: '菜单添加成功',
          type: 'success'
        })
        // 刷新出新的菜单
        this.getMenus()
        this.dialogVisible = false
        // 设置需要默认展开的菜单
        this.expandedKey = [this.category.parentCid]
      })
    },
    editCategory () {
      let {catId, icon, productUnit, name} = this.category
      this.$http({
        url: this.$http.adornUrl('/product/category/update'),
        method: 'post',
        data: this.$http.adornData({catId, icon, productUnit, name}, false)
      }).then(({status}) => {
        if (status === 200) {
          this.$message({
            message: '菜单修改成功',
            type: 'success'
          })
        // 刷新出新的菜单
          this.getMenus()
          this.dialogVisible = false
        // 设置需要默认展开的菜单
          this.expandedKey = [this.category.parentCid]
        }
      })
    },
    remove (node, data) {
      let ids = [data.catId]
      this.$confirm(`是否删除【${data.name}】菜单?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.$http({
            url: this.$http.adornUrl('/product/category/delete'),
            method: 'post',
            data: this.$http.adornData(ids, false)
          }).then(({data}) => {
            this.$message({
              message: '菜单删除成功',
              type: 'success'
            })
            // 刷新出新的菜单
            this.getMenus()
            // 设置需要默认展开的菜单
            this.expandedKey = [node.parent.data.catId]
          })
        })
        .catch(() => {
        })
    },
    submitData () {
      if (this.dialogType === 'add') {
        this.addCategory()
      }
      if (this.dialogType === 'edit') {
        this.editCategory()
      }
    },
    edit (data) {
      this.dialogType = 'edit'
      this.dialogVisible = true
      this.category.catId = data.catId
      this.dialogTitle = '编辑'
      this.$http({
        url: this.$http.adornUrl(`/product/category/info/${data.catId}`),
        method: 'get'
      }).then(({data}) => {
        this.category.name = data.data.name
        this.category.icon = data.data.icon
        this.category.productUnit = data.data.productUnit
        this.category.parentCid = data.data.parentCid
      })
    }

  },
  created () {
    this.getMenus()
  }
}
</script>
