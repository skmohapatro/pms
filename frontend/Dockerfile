# Build stage
FROM node:18-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# Build argument for API URL
ARG API_URL=http://localhost:8080/api
ENV API_URL=$API_URL

# Replace API URL in the source before building
RUN sed -i "s|http://localhost:8080/api|${API_URL}|g" src/app/services/api.service.ts

RUN npm run build -- --configuration=production

# Runtime stage
FROM nginx:alpine
COPY --from=build /app/dist/frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
