# Stage 1: Build the Angular application
FROM node:20-alpine AS builder

# Set working directory
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build and verify output
RUN npm run build --configuration=production && \
    echo "Listing dist directory:" && \
    ls -la dist/frontend/browser && \
    echo "Contents of dist/frontend/browser:" && \
    find dist/frontend/browser -type f

# Stage 2: Serve the application
FROM nginx:1.25-alpine

# Copy nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Create directory and set permissions
RUN mkdir -p /usr/share/nginx/html && \
    chown -R nginx:nginx /usr/share/nginx/html

# Copy built application
COPY --from=builder --chown=nginx:nginx /app/dist/frontend/browser/* /usr/share/nginx/html/

# Verify the files are copied
RUN ls -la /usr/share/nginx/html

# Expose port 80
EXPOSE 80

# Health check (using root path)
HEALTHCHECK --interval=30s --timeout=3s \
    CMD wget --quiet --tries=1 --spider http://localhost:80/ || exit 1

# Start nginx in foreground
CMD ["nginx", "-g", "daemon off;"]