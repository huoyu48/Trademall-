# OrderFlow 常用命令入口
# 用法：make <target>，或直接看 make help

.PHONY: help infra build run stop test clean

MVN ?= ./mvnw

help: ## 显示所有可用命令
	@echo "OrderFlow 工程命令："
	@echo "  make infra    仅启动中间件（MySQL/Redis/RabbitMQ）"
	@echo "  make build    构建后端 jar + 前端 dist"
	@echo "  make run      一键启动全栈（docker compose）"
	@echo "  make stop     停止全栈并释放资源"
	@echo "  make test     运行端到端冒烟测试"
	@echo "  make clean    清理构建产物"

infra: ## 仅启动中间件
	docker compose up -d mysql redis rabbitmq

build: ## 构建后端 jar + 前端静态产物
	cd web && npm install && npm run build
	$(MVN) -DskipTests clean package

run: ## 一键启动全栈
	docker compose up -d --build

stop: ## 停止全栈
	docker compose down

test: ## 端到端冒烟测试
	./scripts/smoke-test.sh

clean: ## 清理构建产物
	$(MVN) clean
	rm -rf web/dist
