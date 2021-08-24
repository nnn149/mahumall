<template>
  <el-tree :data="data" :props="defaultProps" @node-click="handleNodeClick"></el-tree>

</template>
<script>
export default {
  data () {
    return {
      data: [],
      defaultProps: {
        children: 'children',
        label: 'label'
      }
    }
  },
  activated () {
    this.getMenus()
  },
  methods: {
    handleNodeClick (data) {
      console.log(data)
    },
    getMenus () {
      this.$http({
        url: this.$http.adornUrl('/product/category/list/tree'),
        method: 'get'
      }).then(({data}) => {
        if (data && data.code === 0) {
          this.data = data.data
        } else {
          this.dataList = []
        }
      })
    }
  }
}
</script>
