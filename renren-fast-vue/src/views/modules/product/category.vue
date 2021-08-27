<template>
  <div>
    <el-tree :data="menus"
             :props="defaultProps"
             :expand-on-click-node="false"
             show-checkbox
             node-key="catId"
             draggable
             :allow-drop="allowDrop"
             @node-drop="handleDrop"
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
  data() {
    return {
      updateNodes: [],
      dragNodeMaxDepth: 0,
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
  activated() {
    this.getMenus()
  },
  methods: {

    getMenus() {
      this.$http({
        url: this.$http.adornUrl('/product/category/list/tree'),
        method: 'get'
      }).then(({data}) => {
        this.menus = data.data
      })
    },
    append(data) {
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
    addCategory() {
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
    editCategory() {
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
    remove(node, data) {
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
    submitData() {
      if (this.dialogType === 'add') {
        this.addCategory()
      }
      if (this.dialogType === 'edit') {
        this.editCategory()
      }
    },
    edit(data) {
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
    },
    allowDrop(draggingNode, dropNode, type) {
      // 最多允许三级菜单
      let maxDepth = 3
      this.dragNodeMaxDepth = draggingNode.level
      this.countNodeLevel(draggingNode)
      let nowDepth = this.dragNodeMaxDepth - draggingNode.level + 1
      if (type === 'inner') {
        return dropNode.level + nowDepth <= maxDepth
      } else {
        return dropNode.parent.level + nowDepth <= maxDepth
      }
    },
    handleDrop(draggingNode, dropNode, dropType, ev) {
      this.updateNodes.splice(0, this.updateNodes.length)
      let pCid = 0
      let level = 0
      let sortNode = null
      if (dropType === 'inner') {
        pCid = dropNode.data.catId
        level = dropNode.level + 1
        sortNode = dropNode
      } else {
        pCid = dropNode.data.parentCid
        level = dropNode.level
        sortNode = dropNode.parent
      }
      for (let i = 0; i < sortNode.childNodes.length; i++) {
        // 判断前拖拽节点是否需要更改父节点和对被拖拽节点同层所有节点进行排序，只有遍历的节点是被拖拽节点时才需要更改父节点。
        if (sortNode.childNodes[i].data.catId !== draggingNode.data.catId) {
          // 不是当前拖拽节点，不用重新设置父节点和层级
          this.updateNodes.push({catId: sortNode.childNodes[i].data.catId, sort: i})
        } else {
          this.updateNodes.push({catId: sortNode.childNodes[i].data.catId, sort: i, parentCid: pCid, catLevel: level})
          // 重新设置当前节点的所有子节点的level，只能传入sortNode.childNodes[i]而不能用draggingNode。应为前者里面是最新的属性，影响level的值
          this.updateChildNodeLevel(sortNode.childNodes[i])
        }
      }
      this.$http({
        url: this.$http.adornUrl('/product/category/update/sort'),
        method: 'post',
        data: this.$http.adornData(this.updateNodes, false)
      }).then(({data}) => {
        this.$message({
          message: '菜单移动成功',
          type: 'success'
        })
        // 刷新出新的菜单
        this.getMenus()
        // 设置需要默认展开的菜单,打开拖动前和拖动到的菜单
        this.expandedKey = [draggingNode.data.parentCid, dropNode.parent.data.catId]
      })
    },
    // 计算子节点的层级
    updateChildNodeLevel(node) {
      if (node.childNodes != null && node.childNodes.length > 0) {
        for (let i = 0; i < node.childNodes.length; i++) {
          this.updateNodes.push({catId: node.childNodes[i].data.catId, catLevel: node.childNodes[i].level})
          this.updateChildNodeLevel(node.childNodes[i])
        }
      }
    },
    // 查找某节点的子节点的最大深度
    countNodeLevel(node) {
      if (node.childNodes != null && node.childNodes.length > 0) {
        for (let i = 0; i < node.childNodes.length; i++) {
          if (node.childNodes[i].level > this.dragNodeMaxDepth) {
            this.dragNodeMaxDepth = node.childNodes[i].level
          }
          this.countNodeLevel(node.childNodes[i])
        }
      }
    }

  },
  created() {
    this.getMenus()
  }
}
</script>
