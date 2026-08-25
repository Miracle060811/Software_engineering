<template>
  <div class="post-create-page">
    <PageHeader
      title="发布游记"
      subtitle="分享你的旅行故事与精彩瞬间"
      :icon="Edit"
      :breadcrumbs="[
        { label: '首页', to: '/' },
        { label: '社区', to: '/community' },
        { label: '发布游记' }
      ]"
    />

    <el-card class="create-card">
      <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="给你的游记起个吸引人的标题..." maxlength="100" show-word-limit size="large" />
        </el-form-item>

        <el-form-item label="目的地">
          <el-input v-model="form.destination" placeholder="如：云南大理" size="large" />
        </el-form-item>

        <el-form-item label="标签">
          <div class="tags-input">
            <el-tag v-for="tag in form.tags" :key="tag" closable @close="removeTag(tag)" style="margin-right:8px;margin-bottom:4px">
              {{ tag }}
            </el-tag>
            <el-input v-if="tagInputVisible" ref="tagInputRef" v-model="tagInputValue" size="small" style="width:100px"
              @keyup.enter="confirmTag" @blur="confirmTag" />
            <el-button v-else size="small" @click="showTagInput">+ 添加标签</el-button>
          </div>
        </el-form-item>

        <el-form-item label="图片">
          <el-upload
            ref="uploadRef"
            :action="uploadUrl"
            :headers="uploadHeaders"
            list-type="picture-card"
            :on-success="handleUploadSuccess"
            :on-remove="handleUploadRemove"
            :before-upload="beforeUpload"
            multiple
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">支持 JPG/PNG/WebP/GIF，每张不超过 10MB，上传前自动压缩</div>
        </el-form-item>

        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="14"
            placeholder="分享你的旅行故事、景点感受、美食推荐...&#10;&#10;支持 Markdown 格式：&#10;**粗体**  *斜体*  ## 标题  - 列表"
            maxlength="10000"
            show-word-limit
          />
          <div class="format-hints">
            <el-tag size="small" type="info" v-for="hint in ['**粗体**','*斜体*','## 标题','- 列表','> 引用']" :key="hint" style="margin:2px;cursor:pointer" @click="insertFormat(hint)">
              {{ hint }}
            </el-tag>
          </div>
        </el-form-item>

        <el-form-item label="可见范围">
          <el-radio-group v-model="form.visibility">
            <el-radio :value="0">公开</el-radio>
            <el-radio :value="1">仅关注者可见</el-radio>
            <el-radio :value="2">私密</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" size="large" @click="submitPost">发布游记</el-button>
          <el-button :loading="submitting" size="large" @click="saveDraft">保存草稿</el-button>
          <el-button size="large" @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Edit, Plus } from "@element-plus/icons-vue";
import request from "@/utils/request";
import PageHeader from "@/components/PageHeader.vue";
import { optimizeImageForUpload } from "@/utils/imageUpload";

const router = useRouter();
const route = useRoute();
const formRef = ref(null);
const uploadRef = ref(null);
const tagInputRef = ref(null);
const tagInputVisible = ref(false);
const tagInputValue = ref("");
const submitting = ref(false);
const uploadedImages = ref([]);
const editingPostId = computed(() => route.query.draftId || route.query.postId || "");

const form = ref({
  title: "",
  content: "",
  destination: "",
  tags: [],
  visibility: 0,
});

const rules = {
  title: [{ required: true, message: "请输入游记标题", trigger: "blur" }],
  content: [{ required: true, message: "请输入游记内容", trigger: "blur" }],
};

const uploadUrl = import.meta.env.VITE_API_BASE_URL
  ? import.meta.env.VITE_API_BASE_URL + "/api/file/upload"
  : "/api/file/upload";

const uploadHeaders = computed(() => {
  const token = localStorage.getItem("token");
  return token ? { Authorization: "Bearer " + token } : {};
});

const beforeUpload = async (file) => {
  try {
    return await optimizeImageForUpload(file);
  } catch (error) {
    ElMessage.error(error.message || "图片压缩失败");
    return false;
  }
};

const handleUploadSuccess = (res) => {
  const url = res?.url || res?.data?.url || (typeof res?.data === "string" ? res.data : "");
  if (url) {
    uploadedImages.value.push(url);
  } else {
    ElMessage.error("图片上传结果异常");
  }
};

const handleUploadRemove = (file) => {
  const url = file.response?.url || file.response?.data?.url || file.response?.data || file.url;
  uploadedImages.value = uploadedImages.value.filter((u) => u !== url);
};

const parseImages = (images) => {
  if (!images) return [];
  return Array.isArray(images)
    ? images
    : images.split(",").map((item) => item.trim()).filter(Boolean);
};

const insertFormat = (hint) => {
  form.value.content += " " + hint + " ";
};

const showTagInput = async () => {
  tagInputVisible.value = true;
  await nextTick();
  tagInputRef.value?.input?.focus();
};

const confirmTag = () => {
  const val = tagInputValue.value.trim();
  if (val && !form.value.tags.includes(val) && form.value.tags.length < 10) {
    form.value.tags.push(val);
  }
  tagInputVisible.value = false;
  tagInputValue.value = "";
};

const removeTag = (tag) => {
  form.value.tags = form.value.tags.filter((t) => t !== tag);
};

const buildPostData = () => ({
  title: form.value.title.trim(),
  content: form.value.content.trim(),
  destination: form.value.destination.trim(),
  images: uploadedImages.value.join(","),
  tags: form.value.tags.join(","),
  visibility: form.value.visibility,
});

const submitPost = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    let result;
    if (editingPostId.value) {
      result = await request.put(`/api/post/${editingPostId.value}`, buildPostData());
    } else {
      result = await request.post("/api/post/create", buildPostData());
    }
    if (result?.status === 2) {
      ElMessage.warning(result.rejectReason || "游记未通过 AI 审核");
    } else if (result?.status === 1) {
      ElMessage.success("游记已通过 AI 审核并发布");
    } else {
      ElMessage.info("游记已提交 AI 审核");
    }
    router.push("/community");
  } catch (e) {
  } finally {
    submitting.value = false;
  }
};

const saveDraft = async () => {
  if (!form.value.title && !form.value.content) {
    ElMessage.warning("请至少输入标题或内容");
    return;
  }
  submitting.value = true;
  try {
    const payload = { ...buildPostData(), status: 3 };
    if (editingPostId.value) {
      await request.put(`/api/post/${editingPostId.value}`, payload);
    } else {
      await request.post("/api/post/create", payload);
    }
    ElMessage.success("草稿已保存");
    router.push("/community");
  } catch (e) {
  } finally {
    submitting.value = false;
  }
};

const loadDraft = async () => {
  if (!editingPostId.value) return;
  try {
    const data = await request.get(`/api/post/${editingPostId.value}`);
    form.value = {
      title: data.title || "",
      content: data.content || "",
      destination: data.destination || "",
      tags: data.tags ? data.tags.split(",").map((item) => item.trim()).filter(Boolean) : [],
      visibility: data.visibility ?? 0,
    };
    uploadedImages.value = parseImages(data.images);
  } catch (e) {}
};

onMounted(loadDraft);
</script>

<style scoped>
.post-create-page { max-width: 860px; margin: 0 auto; }
.create-card { border-radius: 16px; border: 1px solid #F0F2F5; }
.tags-input { display: flex; flex-wrap: wrap; align-items: center; }
.upload-tip { font-size: 12px; color: #A0A0B8; margin-top: 4px; }
.format-hints { margin-top: 6px; display: flex; flex-wrap: wrap; gap: 4px; }
</style>
