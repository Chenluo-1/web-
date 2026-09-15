<template>
  <div class="search-bar">
    <el-input
      v-model="keyword"
      placeholder="搜索商品（支持同义词与错别字容错）"
      clearable
      @keyup.enter="doSearch"
      @input="onInput"
      @focus="onFocus"
      @blur="onBlur"
    >
      <template #append>
        <el-button @click="doSearch">搜索</el-button>
      </template>
    </el-input>

    <div v-show="panelVisible" class="panel">
      <!-- 联想词 -->
      <template v-if="suggests.length">
        <div class="panel-title">联想</div>
        <div
          v-for="s in suggests"
          :key="'s-' + s"
          class="item"
          @mousedown.prevent="pick(s)"
        >{{ s }}</div>
      </template>
      <!-- 热门搜索 -->
      <template v-if="hotWords.length">
        <div class="panel-title">热门搜索</div>
        <div class="hot-area">
          <el-tag
            v-for="h in hotWords"
            :key="'h-' + h.keyword"
            class="hot-tag"
            type="info"
            @mousedown.prevent="pick(h.keyword)"
          >{{ h.keyword }}</el-tag>
        </div>
      </template>
      <el-empty v-if="!suggests.length && !hotWords.length" description="暂无历史搜索" :image-size="50" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import searchHttp from '@/api/searchHttp'

const router = useRouter()
const keyword = ref('')
const suggests = ref([])
const hotWords = ref([])
const panelVisible = ref(false)

const loadHot = async () => {
  try {
    const res = await searchHttp.hotWords(10)
    hotWords.value = res.data
  } catch (e) { /* 静默 */ }
}

const onFocus = () => {
  panelVisible.value = true
  loadHot()
}

const onBlur = () => {
  setTimeout(() => { panelVisible.value = false }, 200)
}

const onInput = async () => {
  const kw = keyword.value.trim()
  if (!kw) {
    suggests.value = []
    return
  }
  try {
    const res = await searchHttp.suggest(kw)
    suggests.value = res.data
  } catch (e) {
    suggests.value = []
  }
}

const pick = kw => {
  keyword.value = kw
  doSearch()
}

const doSearch = () => {
  const kw = keyword.value.trim()
  if (!kw) return
  panelVisible.value = false
  router.push('/search?keyword=' + encodeURIComponent(kw))
}

onMounted(loadHot)
</script>

<style scoped>
.search-bar {
  position: relative;
  width: 380px;
}

.panel {
  position: absolute;
  top: 40px;
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  padding: 8px 12px;
  z-index: 100;
  max-height: 300px;
  overflow-y: auto;
}

.panel-title {
  color: #909399;
  font-size: 12px;
  margin: 4px 0;
}

.item {
  padding: 5px 4px;
  cursor: pointer;
  border-radius: 3px;
  font-size: 13px;
}

.item:hover {
  background: #f5f7fa;
}

.hot-area {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}

.hot-tag {
  cursor: pointer;
}
</style>
