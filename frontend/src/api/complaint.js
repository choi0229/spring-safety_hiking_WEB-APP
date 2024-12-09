import { apiInstance } from "./index.js";
const api = apiInstance();
import router from "@/router/index.js";
import axios from 'axios';

//complaintList 가져오기 요청
export async function getComplaintList() {
  try { 
    const response = await axios.get('/api/complaint/list');
    console.log("you can use getComplaintList", response.data);
    return response.data;
  } catch (err) {
    console.error(err); // 에러를 콘솔에 출력
  }
}

//complaintList 날짜 최신순으로 가져오기 요청
export async function getRecentComplaintList() {
  try { 
    const response = await axios.get('/api/complaint/listRecent');
    console.log("you can use getRecentComplaintList", response.data);
    return response.data;
  } catch (err) {
    console.error(err); // 에러를 콘솔에 출력
  }
}

//complaintList 글쓴이아이디에 해당하는 리스트 최신순으로 가져오기 요청
export async function getComplaintByUserId(userId) {
  try { 
    const response = await axios.get(`/api/complaint/mine/${userId}`);
    //console.log("getComplaintByUserId", response.data);
    return response.data;
  } catch (err) {
    console.error(err); // 에러를 콘솔에 출력
  }
}

//complaintList 글쓴이아이디에 해당하는 리스트 최신순으로 가져오기 요청
export async function getComplaintListByInst(institution) {
  try { 
    const response = await axios.get(`/api/complaint/myList/${institution}`);
    //console.log("getComplaintListByInst", response.data);
    return response.data;
  } catch (err) {
    console.error(err); // 에러를 콘솔에 출력
  }
}

//pathList 날짜 최신순으로 가져오기 요청
export async function getPathList() {
  try { 
    const response = await axios.get('/api/pathList');
    console.log("you can use getPathList", response.data);
    return response.data;
  } catch (err) {
    console.error(err); // 에러를 콘솔에 출력
  }
}

// 민원글 작성 요청
export async function insertComplaint(data) {
  try {
    const response = await axios.post('/api/complaint/insert', data);
    console.log('응답 상태 코드:', response.status); // 응답 상태 코드 확인
    console.log('민원글 작성에 대한 응답 데이터:', response.data); 
    return response.data;
  } catch (err) {
    console.error('API 요청 중 오류 발생:', err);
    throw err;
  }
}

// 민원글 번호에 따른 민원글 한개 가져오기 요청
export async function getComplaintByNo(complaintNo) {
  try {
    const response = await axios.get(`/api/complaint/one/${complaintNo}`);
    console.log("API response:", response);
    return response.data;
  } catch (err) {
    console.error(err); // 에러를 콘솔에 출력
  }
}

// 유저 아이디에 따른 유저정보 한개 가져오기 요청
export async function getUserById(userId) {
  try {
    const response = await axios.get(`/api/userInfo/${userId}`);
    console.log("API response:", response);
    return response.data;
  } catch (err) {
    console.error(err); // 에러를 콘솔에 출력
  }
  
}

// 코스 한개씩 가져오기 요청
export async function getCourseById(courseId) {
  try {
    const response = await axios.get(`/api/course/${courseId}`);
    console.log("API response:", response);
    return response.data;
  } catch (err) {
    console.error(err); // 에러를 콘솔에 출력
  }
}


// 민원글 번호에 따른 글 삭제 요청
export async function deleteComplaintByNo(complaintNo) {
  try {
    const response = await axios.delete(`/api/complaint/delete/${complaintNo}`);
    return response.data;
  } catch (err) {
    console.error(err);
  }
}

// 글 업데이트
export async function updateComplaintByNo(complaintNo, data) {
  try {
    const response = await axios.patch(`/api/complaint/update/${complaintNo}`, data);
    return response.data;
  } catch (err) {
    console.error(err);
  }
}

//특정 유저의 complaintList를 가져오기 요청
export async function getUserPostList(userNo) {
  try { 
    const response = await api.get(`/auth/userpostlist/${userNo}`);
    console.log(userNo);
    console.log("you can use getUserPostList", response.data);
    return response.data;
  } catch (err) {
    console.error(err); // 에러를 콘솔에 출력
  }
}

//게시글 추가 및 
export async function insertPostAndKeyword(postData, keywordData) {
  const formData = new FormData();
  formData.append('post', new Blob([JSON.stringify(postData)], { type: 'application/json' }));
  formData.append('postkeyword', new Blob([JSON.stringify(keywordData)], { type: 'application/json' }));

  try {
    const response = await api.post('/auth/insert', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  } catch (err) {
    console.error(err);
    if (err.response && err.response.status === 401) {
      // 로그인 페이지로 이동
      alert("로그인 후 이용하실 수 있습니다.")
      router.push({ path: '/login' });
    }
    throw err;
  }
} 

// 이미지 저장요청
export async function saveImage(formData) {
  try {
    const response = await api.post('/auth/upload-image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    console.log('서버 응답:', response.data);
    return response.data;
  } catch (err) {
    console.error('이미지 업로드 중 오류 발생:', err); // 에러를 콘솔에 출력
  }
}

// 민원글 번호에 따른 민원처리정보 한개 가져오기 요청
export async function getProcessingByNo(processingComplaintNo) {
  try {
    const response = await axios.get(`/api/complaint/processing/${processingComplaintNo}`);
    console.log("API response:", response);
    return response.data;
  } catch (err) {
    console.error(err); // 에러를 콘솔에 출력
  }
}

// 민원처리정보 작성 요청
export async function insertProcessing(data) {
  try {
    const response = await axios.post('/api/complaint/insertProcessing', data);
    console.log('응답 상태 코드:', response.status); // 응답 상태 코드 확인
    console.log('민원처리정보 작성에 대한 응답 데이터:', response.data); 
    return response.data;
  } catch (err) {
    console.error('API 요청 중 오류 발생:', err);
    throw err;
  }
}

// 민원처리정보 업데이트
export async function updateProcessingByNo(processingComplaintNo, data) {
  try {
    const response = await axios.patch(`/api/complaint/updateProcessing/${processingComplaintNo}`, data);
    return response.data;
  } catch (err) {
    console.error(err);
  }
}