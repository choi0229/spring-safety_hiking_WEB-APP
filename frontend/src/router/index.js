import { createRouter, createWebHistory } from 'vue-router'
import MainView from '@/views/MainView.vue'
import HelpCallView from '../views/HelpCallView.vue';

const routes = [
  {
    path: '/',
    name: 'home',
    component: MainView
  },
  {
    path: '/about',
    name: 'about',
    component: () => import(/* webpackChunkName: "about" */ '../views/AboutView.vue')
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/mountain',
    name: 'mountain',
    component: () => import('../views/MountainView.vue')
  },
  {
    path: '/mountaindetail',
    name: 'mountaindetail',
    component: () => import('../views/MountainDetailView.vue'),
    props: route => ({
      course: route.query.course
    }),
  },
  {
    path: '/weather',
    name: 'weather',
    component: () => import('../views/WeatherView.vue')
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/signup',
    name: 'signup',
    component: () => import('../views/JoinMembershipView.vue')
  },
  {
    path: '/mountaindetail2',
    name: 'mountaindetail2',
    component: () => import('../views/MountainDetailView2.vue')
  },
  {
    path: '/complaintList',
    name: 'complaintList',
    component: () => import('../views/ComplaintListView.vue')
  },
  {
    path: '/complaint',
    name: 'complaint',
    component: () => import('../views/ComplaintView.vue')
  },
  {
    path: '/complaintinfo',
    name: 'complaintinfo',
    component: () => import('../views/ComplaintInfoView.vue')
  },
  {
    path: '/complaintMobile',
    name: 'complaintMobile',
    component: () => import('../views/ComplaintViewMobile.vue')
  },
  {
    path: '/complaintInfoMobile',
    name: 'complaintInfoMobile',
    component: () => import('../views/ComplaintInfoViewMobile.vue')
  },
  {
    path: '/complaintListMobile',
    name: 'complaintListMobile',
    component: () => import('../views/ComplaintListViewMobile.vue')
  },
  {
    path: '/mobilemainview',
    name: 'mobilemainview',
    component: () => import('../views/MobileMainView.vue'),
  },
  {
    path: '/mobilemountaindetailview',
    name: 'mobilemountaindetailview',
    component: () => import('../views/MobileMountainDetailView.vue'),
    props: route => ({
      course: route.query.course
    }),
  },
  {
    path: '/3d',
    name: '3d',
    component: () => import('../views/3dView.vue')
  },
  {
    path: '/processing',
    name: 'processing',
    component: () => import('../views/ProcessingView.vue')
  },
  {
    path: '/manageinfo',
    name: 'manageinfo',
    component: () => import('../views/ManageInfoView.vue')
  },
  {
    path: '/manageList',
    name: 'manageList',
    component: () => import('../views/ManageListView.vue')
  },
  {
    path: '/heatmap',
    name: 'heatmap',
    component: () => import('../views/HeatmapView.vue')
  },
  {
    path: '/mobilecourseview',
    name: 'mobilecourseview',
    component: () => import('../views/MobileCourseView.vue')
  },
  {
    path: '/mobilecourseinfoview',
    name: 'mobilecourseinfoview',
    component: () => import('../views/MobileCourseInfoView.vue')
  },
  {
    path: '/clMapMobile',
    name: 'complaintListMapMobile',
    component: () => import('../views/ComplaintListMapViewMobile.vue')
  },
  {
    path: '/community',
    name: 'community',
    component: () => import('../views/CommunityView.vue')
  },
  {
    path: '/mobilecommunity',
    name: 'mobilecommunity',
    component: () => import('../views/MobileCommunityView.vue')
  },
  {
    path: '/record',
    name: 'record',
    component: () => import('../views/RecordView.vue')
  },
  {
    path: '/recordImg',
    name: 'recordImg',
    component: () => import('../views/RecordImgView.vue')
  },
  {
    path: '/myCommunity',
    name: 'myCommunity',
    component: () => import('../views/MyCommunity.vue')
  },
  {
    path: '/mobileCoursePreview',
    name: 'mobileCoursePreview',
    component: () => import('../views/MobileCoursePreview.vue'),
  },
  {
    path: '/helpcall',
    name: 'helpcall',
    component: HelpCallView,
  },
  {
  path: '/writePost',
  name: 'writePost',
  component: () => import('../views/WritePostView.vue'),
  },
  {
    path: '/mobileCourse3d',
    name: 'mobileCourse3d',
    component: () => import('../views/MobileCourse3D.vue'),
  },
  {
    path: '/webCourse3d',
    name: 'webCourse3d',
    component: () => import('../views/WebCourse3D.vue'),
  },
  {
    path: '/comparecourse',
    name: 'comparecourse',
    component: () => import('../views/CompareCourseView.vue'),
  },
  {
    path: '/balance',
    name: 'balance',
    component: () => import('../views/BalanceView.vue'),
  },
    {
    path: '/communityDetailView',  // ":id"는 동적 매개변수로 게시글 ID를 나타냅니다.
    name: 'communityDetailView',
    component: () => import('../views/CommunityDetailView.vue')
  },
  {
    path: '/realtime',  // ":id"는 동적 매개변수로 게시글 ID를 나타냅니다.
    name: 'realtime',
    component: () => import('../views/RealTimePeopleView.vue')
  },
  {
    path: '/realtimeheatmap',  // ":id"는 동적 매개변수로 게시글 ID를 나타냅니다.
    name: 'realtimeheatmap',
    component: () => import('../views/RealTimePeopleHeatmapView.vue')
  },
  {
    path: '/mobilerealtimeheatmap',  // ":id"는 동적 매개변수로 게시글 ID를 나타냅니다.
    name: 'mobilerealtimeheatmap',
    component: () => import('../views/MobileRealTimePeopleHeatmapView.vue')
  },
  {
    path: '/accident',  // ":id"는 동적 매개변수로 게시글 ID를 나타냅니다.
    name: 'accident',
    component: () => import('../views/AccidentView.vue')
  },
  {
    path: '/mobileaccident',  // ":id"는 동적 매개변수로 게시글 ID를 나타냅니다.
    name: 'mobileaccident',
    component: () => import('../views/MobileAccidentView.vue')
  },
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

export default router
