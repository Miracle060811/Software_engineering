<template>
  <div class="post-create-page">
    <el-card class="create-card">
      <template #header>
        <span class="card-title">发布游记</span>
      </template>
      <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="给你的游记起个吸引人的标题..."
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="目的地">
          <el-input v-model="form.destination" placeholder="如：云南大理" />
        </el-form-item>

        <el-form-item label="标签">
          <div class="tags-input">
            <el-tag
              v-for="tag in form.tags"
              :key="tag"
              closable
              @close="removeTag(tag)"
              style="margin-right: 8px; margin-bottom: 4px"
            >
              {{ tag }}
            </el-tag>
            <el-input
              v-if="tagInputVisible"
              ref="tagInputRef"
              v-model="tagInputValue"
              size="small"
              style="width: 100px"
              @keyup.enter="confirmTag"
              @blur="confirmTag"
            />
            <el-button v-else size="small" @click="showTagInput"
              >+ 添加标签</el-button
            >
          </div>
        </el-form-item>

        <el-form-item label="图片链接">
          <el-input
            v-model="form.imagesInput"
            type="textarea"
            :rows="3"
            placeholder="填写图片URL，多张图片用逗号分隔（可选）&#10;如：https://example.com/img1.jpg,https://example.com/img2.jpg"
          />
          <div class="image-preview" v-if="previewImages.length">
            <img
              v-for="(img, idx) in previewImages"
              :key="idx"
              :src="img"
              class="preview-img"
              :alt="`预览${idx + 1}`"
            />
          </div>
          <el-button link type="primary" @click="previewImgs"
            >预览图片</el-button
          >
        </el-form-item>

        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="12"
            placeholder="分享你的旅行故事、景点感受、美食推荐..."
            maxlength="10000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitPost"
            >发布游记</el-button
          >
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import request from "@/utils/request";

const router = useRouter();
const formRef = ref(null);
const tagInputRef = ref(null);
const tagInputVisible = ref(false);
const tagInputValue = ref("");
const submitting = ref(false);

const form = ref({
  title: "",
  content: "",
  destination: "",
  imagesInput: "",
  tags: [],
});

const rules = {
  title: [{ required: true, message: "请输入游记标题", trigger: "blur" }],
  content: [{ required: true, message: "请输入游记内容", trigger: "blur" }],
};

const previewImages = ref([]);

const previewImgs = () => {
  previewImages.value = form.value.imagesInput
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
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

const submitPost = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    const images = form.value.imagesInput
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean)
      .join(",");
    await request.post("/api/post/create", {
      title: form.value.title,
      content: form.value.content,
      destination: form.value.destination,
      images,
      tags: form.value.tags.join(","),
    });
    ElMessage.success("游记发布成功，等待审核");
    router.push("/community");
  } catch (e) {
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.post-create-page {
  max-width: 860px;
  margin: 0 auto;
}
.create-card {
  border-radius: 12px;
}
.card-title {
  font-size: 18px;
  font-weight: 600;
}
.tags-input {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}
.image-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
  margin-bottom: 4px;
}
.preview-img {
  width: 100px;
  height: 80px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #e8e8e8;
}
</style>
