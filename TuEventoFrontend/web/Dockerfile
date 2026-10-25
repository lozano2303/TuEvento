# Build stage
FROM node:20.19-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --silent
COPY . .
# Vite inlines VITE_* values at build time — pass them as build args
ARG VITE_API_URL
ARG VITE_GOOGLE_CLIENT_ID
ARG VITE_GATEWAY_URL
ENV VITE_API_URL=$VITE_API_URL \
    VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID \
    VITE_GATEWAY_URL=$VITE_GATEWAY_URL
RUN npm run build

# Production stage
FROM nginx:1.29.3-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
