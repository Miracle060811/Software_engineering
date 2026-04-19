import { createRouter, createWebHistory } from "vue-router";
import Home from "../views/Home.vue";
import FlightList from "../views/flight/FlightList.vue";
import OrderList from "../views/order/OrderList.vue";

const routes = [
  {
    path: "/",
    name: "Home",
    component: Home,
  },
  {
    path: "/flight-search",
    name: "FlightList",
    component: FlightList,
  },
  {
    path: "/my-orders",
    name: "OrderList",
    component: OrderList,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
