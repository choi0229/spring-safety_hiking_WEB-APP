import { ref } from "vue";
import { defineStore } from "pinia";
import { getComplaintList, getUserPostList, getComplaintListByInst, getComplaintByNo, getRecentComplaintList, getPathList, getComplaintByUserId } from '@/api/complaint';

// 민원글 리스트 가져오는 상태
export const useComplaintListStore = defineStore('complaintlist', () => {
  const complaintlist = ref([]);
  async function fetchComplaintList() {
    console.log("Fetching complaintlist ===");
    complaintlist.value = await getComplaintList();
    console.log("postlist--",complaintlist.value);
  }

  return { complaintlist, fetchComplaintList }
})

// 민원글 리스트 날짜 최신순으로 가져오는 상태
export const useRecentComplaintListStore = defineStore('recentcomplaintlist', () => {
  const recentcomplaintlist = ref([]);
  async function fetchRecentComplaintList() {
    //console.log("Fetching recentcomplaintlist ===");
    recentcomplaintlist.value = await getRecentComplaintList();
    //console.log("recentpostlist--",recentcomplaintlist.value);
  }

  return { recentcomplaintlist, fetchRecentComplaintList }
})

export const useMyComplaintListStore = defineStore('mycomplaintlist', () => {
  const mycomplaintlist = ref([]);
  async function fetchMyComplaintList(userId) {
    //console.log("Fetching mycomplaintlist ===");
    mycomplaintlist.value = await getComplaintByUserId(userId);
    //console.log("recentpostlist--",recentcomplaintlist.value);
  }

  return { mycomplaintlist, fetchMyComplaintList }
})

export const useManagingComplaintListStore = defineStore('managingcomplaintlist', () => {
  const managingcomplaintlist = ref([]);
  async function fetchManagingComplaintList(institution) {
    managingcomplaintlist.value = await getComplaintListByInst(institution);
    console.log("담당업무--",managingcomplaintlist.value);
  }

  return { managingcomplaintlist, fetchManagingComplaintList }
})

// 운동 기록 리스트 날짜 최신순으로 가져오는 상태
export const usePathList = defineStore('pathList', () => {
  const pathList = ref([]);
  async function fetchPathList() {
    //console.log("Fetching recentcomplaintlist ===");
    pathList.value = await getPathList();
    //console.log("recentpostlist--",recentcomplaintlist.value);
  }

  return { pathList, fetchPathList }
})

// 민원글 번호에 따른 민원글 한개 가져오는 상태
export const useComplaintStore = defineStore('complaintByNo', () => {
  const complaintone = ref({});
  async function fetchComplaintone(postNo) {
    try {
      complaintone.value = await getComplaintByNo(postNo);
    } catch (err) {
      console.error('Error fetching post:', err);
    }
  }
  return { complaintone, fetchComplaintone };
});

// 특정 유저의 게시글 목록 가져오는 상태
export const useUserPostListStore = defineStore('userpostlist', () => {
  const userpostlist = ref([]);
  async function fetchUserPost() {
    const userNo = sessionStorage.getItem('userNo');
    console.log(userNo);
    if (!userNo) {
      console.error('userNo is undefined or null');
      return;
    }
    try {
      console.log("userpostlist -- 유저 포스트 리스트");
      userpostlist.value = await getUserPostList(userNo);
      console.log("userpostlist--", userpostlist.value);
    } catch (err) {
      console.error('Error fetching userpostlist:', err);
    }
  }

  return { userpostlist, fetchUserPost }
})
