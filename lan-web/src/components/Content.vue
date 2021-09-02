<template>
  <div class="home">
    <el-row style="height: 100%">
      <el-col :span="3" style="height: 1px" />
      <el-col :span="18" class="home-content">
        <!-- Upload trigger area -->
        <el-upload
          ref="upload"
          class="upload"
          drag
          action="void"
          multiple
          :http-request="uploadRequest"
          :on-change="handleOnFileChange"
          :auto-upload="false"
          list-type="picture"
        >
          <div class="el-upload-text">
            {{ $t("dragFilesHere") }}
            <em>{{ $t("clickToSelect") }}</em>
          </div>
        </el-upload>

        <!-- Retry All btn -->
        <div v-show="failedNum > 0" class="reUploadAll">
          <div />
          <div>
            <el-button class="reUploadAll-btn" @click="retryAll">
              {{ $t("retryAll") }}
              <i class="el-icon-refresh" />
            </el-button>
          </div>
        </div>

        <div v-for="(item, i) in items" :key="i" style="width: 70%">
          <transition name="el-fade-in">
            <div
              class="upload-item"
              v-bind:class="{
                'bg-normal': item.status === Status.waiting,
                'bg-uploading': item.status === Status.uploading,
                'bg-success': item.status === Status.success,
                'bg-fail': item.status === Status.fail,
              }"
            >
              <div class="image-info">
                <img class="image" :src="item.url" alt="iamge" />
                <span class="image-name">{{ item.file.name }}</span>
              </div>
              <!-- fail tip -->
              <div v-if="item.status === Status.fail" class="item-mask-fail">
                <el-button
                  type="danger"
                  class="reUpload-btn"
                  round
                  @click="enqueue(item)"
                >
                  {{ $t("retry") }}
                </el-button>
                <el-button
                  type="danger"
                  icon="el-icon-delete"
                  circle
                  @click="removeItem(item)"
                />
              </div>

              <div
                v-show="item.status !== Status.uploading"
                class="item-controls"
              >
                <i
                  class="el-icon-close"
                  style="color: #838283; margin-left: 5px; cursor: pointer"
                  @click="removeItem(item)"
                />
              </div>

              <div v-show="item.status === Status.uploading" class="prograss">
                <el-progress
                  :percentage="item.prograss"
                  :format="progressFormat"
                  :stroke-width="3"
                />
              </div>
            </div>
          </transition>
        </div>
      </el-col>
      <el-col :span="3" />
    </el-row>
  </div>
</template>

<script lang="ts">
import { ElUploadInternalFileDetail } from "element-ui/types/upload";
import Vue from "vue";
import Component from "vue-class-component";

enum Status {
  waiting,
  uploading,
  success,
  fail,
}

class UploadJob {
  file: File;
  url?: string;
  status: Status = Status.waiting;
  progress = 0;

  constructor(file: File, url?: string) {
    this.file = file;
    this.url = url;
  }
}

const UPLOAD_URL = "upload";

@Component
export default class Content extends Vue {
  items: UploadJob[] = [];
  failedNum = 0;
  isRunning = false;
  Status = Status;

  queue: UploadJob[] = [];

  /**
   * El Upload callback.
   *
   * The hook when the state of the file is changed, and it will be called when the file is added,
   * successful or fails.
   */
  handleOnFileChange(file: ElUploadInternalFileDetail): void {
    // we use this callback as an file-added callback.
    // so ignore other status.
    if (file.status != "ready") return;

    // check file type
    if (!file.raw.type.startsWith("image/")) {
      this.$message.warning(this.$i18n.t("nonImageFileWarn").toString());
      return;
    }

    let job = new UploadJob(file.raw, file.url);
    this.items.push(job);
    this.enqueue(job);
  }

  /**
   * El Upload callback.
   */
  uploadRequest(): void {
    // just override default behavior to make sure disable element itself request.
  }

  /**
   *  El Progress Text Formatter.
   */
  progressFormat(): string {
    // simply return empty text
    return "";
  }

  retryAll(): void {
    this.items
      .filter((e: UploadJob) => {
        return e.status === Status.fail;
      })
      .forEach((e) => {
        this.retry(e);
      });
  }

  retry(job: UploadJob): void {
    if (job.status === Status.fail) {
      this.enqueue(job);
    }
  }

  /**
   * Enqueue a upload job. If the queue is not running, start it.
   */
  enqueue(item: UploadJob): void {
    if (item.status === Status.fail) {
      this.failedNum--;
    }
    item.status = Status.waiting;
    this.queue.push(item);
    if (!this.isRunning) {
      this.startUpload();
    }
  }

  removeItem(item: UploadJob): void {
    let i = this.queue.indexOf(item);
    // remove it from the queue, if present
    if (i != -1) {
      if (this.queue[i].status === Status.uploading) {
        // this job is processing, can not remove it
        return;
      }
      this.queue.splice(i, 1);
    }
    // then remove it from list
    i = this.items.indexOf(item);
    if (i != -1) {
      if (item.status === Status.fail) {
        this.failedNum--;
      }
      this.items.splice(i, 1);
    }
  }

  /**
   * Start the upload queue. If it's already started, then do nothing.
   */
  async startUpload(): Promise<void> {
    if (this.isRunning) return;
    this.isRunning = true;

    let job = this.queue.shift();
    while (job) {
      job.status = (await this.uploadItem(job)) ? Status.success : Status.fail;
      if (job.status === Status.fail) {
        this.failedNum++;
        this.$message.error(
          this.$i18n.t("uploadError", { name: job.file.name }).toString()
        );
      }
      job = this.queue.shift();
    }
    this.isRunning = false;
  }

  async uploadItem(job: UploadJob): Promise<boolean> {
    try {
      let formData = new FormData();
      formData.append("image", job.file, job.file.name);
      job.status = Status.uploading;
      const r = await this.axios.post(UPLOAD_URL, formData, {
        headers: { "Content-Type": "multipart/form-data" },
        onUploadProgress: (progressEvent) => {
          if (progressEvent.lengthComputable) {
            job.progress = (progressEvent.loaded / progressEvent.total) * 100;
          }
        },
      });
      return r.status == 201;
    } catch (error) {
      return false;
    }
  }
}
</script>

<style scoped>
.home {
  width: 100%;
  height: 100%;
}
.home-content {
  margin: 50px 0 50px 0;
  padding: 40px 0 40px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 20px;
  border: dashed rgba(0, 16, 31, 0.5) 1px;
}

.upload {
  width: 70%;
  height: 100%;
}

.upload-item {
  display: flex;
  position: relative;
  align-items: center;
  justify-content: space-between;
  border-radius: 20px;
  border: none;
  width: 100%;
  height: 110px;
  margin-top: 16px;
}
.bg-normal {
  background-color: #00000008;
}
.bg-uploading {
  background-color: #00000008;
}
.bg-success {
  background-color: #0dbc7922;
}
.bg-fail {
  background-color: #00000008;
}

.image-info {
  width: 100%;
  display: flex;
  align-items: center;
  margin: 20px;
}
.item-mask-fail {
  display: flex;
  align-items: center;
  justify-content: center;
  position: absolute;
  background-color: rgba(0, 16, 31, 0.2);
  border-radius: 20px;
  width: 100%;
  height: 100%;
  top: 0px;
  right: 0px;
}

.item-controls {
  margin-left: 20px;
  position: absolute;
  top: 10px;
  right: 20px;
}
.prograss {
  position: absolute;
  bottom: 0px;
  right: 20px;
  width: 80%;
  height: 18px;
  left: calc(50% - 40%);
}
@media screen and (max-width: 500px) {
  .image-name {
    width: 45%;
    margin-left: 5px;
    margin-right: 5px;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .image {
    width: 50px;
    height: 50px;
  }

  .reUploadAll {
    width: 70%;
    display: flex;
    justify-content: center;
    margin-top: 10px;
    color: #00101f;
  }
  .reUploadAll-btn {
    border-radius: 16px;
    background-color: #00101f;
    color: #fff;
  }
}
@media screen and (min-width: 500px) {
  .image-name {
    width: 100%;
    margin-left: 20px;
    margin-right: 40px;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .image {
    width: 70px;
    height: 70px;
  }

  .reUploadAll {
    width: 70%;
    display: flex;
    justify-content: space-between;
    margin-right: 30px;
    margin-top: 10px;
    color: #00101f;
  }
  .reUploadAll-btn {
    border-radius: 16px;
    background-color: #00101f;
    color: #fff;
  }
}

.el-upload-text {
  margin: 20px;
}
</style>
