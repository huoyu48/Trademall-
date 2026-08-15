# OrderFlow 前端运行镜像（Nginx 托管静态资源 + 反向代理 /api 到后端）
# 前提：已通过 `npm install && npm run build` 在 dist/ 生成产物
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist /usr/share/nginx/html

EXPOSE 80
