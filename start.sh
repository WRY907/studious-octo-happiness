#!/usr/bin/env bash
# ================================================================
# 华为手机全息数据驾驶舱 - 一键启动脚本 (macOS / Linux)
# ================================================================
set -e
cd "$(dirname "$0")"

echo ""
echo "================================================================"
echo "       🚀 华为手机全息数据驾驶舱 HOLO DATA COCKPIT"
echo "================================================================"
echo ""

# ---------- 1. 环境检查 ----------
echo "[1/4] 检查运行环境..."

if ! command -v java >/dev/null 2>&1; then
  echo "  ❌ 未检测到 Java，请先安装 JDK 8+（https://adoptium.net/）"
  exit 1
fi
echo "  ✅ Java: $(java -version 2>&1 | head -n1)"

if ! command -v node >/dev/null 2>&1; then
  echo "  ❌ 未检测到 Node.js，请先安装 Node.js 16+（https://nodejs.org/）"
  exit 1
fi
echo "  ✅ Node: $(node --version)"

# ---------- 2. 前端依赖 ----------
echo ""
echo "[2/4] 检查前端依赖..."
if [ -d "holo-cockpit-frontend/node_modules" ]; then
  echo "  ✅ 前端依赖已安装，跳过"
else
  echo "  ⏳ 首次运行，正在安装前端依赖（可能需要几分钟）..."
  (cd holo-cockpit-frontend && npm install --registry=https://registry.npmmirror.com)
  echo "  ✅ 前端依赖安装完成"
fi

# ---------- 3. 数据库 ----------
echo ""
echo "[3/4] 数据库说明"
echo "  若尚未初始化数据库，请先执行以下命令（需要本地 MySQL 5.6+）："
echo "      mysql -uroot -p < sql/init.sql"
echo "  并确保 holo-cockpit-backend/application.yml 中的数据库密码正确"
echo ""

# ---------- 4. 启动服务 ----------
echo "[4/4] 启动服务..."
echo "  后端（Spring Boot，端口 8080）与前端（Vite，端口 3000）将分别在新终端启动"
echo ""

if command -v mvn >/dev/null 2>&1; then
  MVN_CMD="mvn spring-boot:run"
else
  MVN_CMD="./mvnw spring-boot:run"
fi

case "$(uname -s)" in
  Darwin*)
    (cd holo-cockpit-backend && osascript -e "tell app \"Terminal\" to do script \"cd $(pwd)/holo-cockpit-backend && $MVN_CMD\"")
    (cd holo-cockpit-frontend && osascript -e "tell app \"Terminal\" to do script \"cd $(pwd)/holo-cockpit-frontend && npm run dev\"")
    ;;
  *)
    gnome-terminal -- bash -c "cd '$PWD/holo-cockpit-backend' && $MVN_CMD; exec bash" 2>/dev/null \
      || xterm -e "cd '$PWD/holo-cockpit-backend' && $MVN_CMD; bash" 2>/dev/null \
      || (cd holo-cockpit-backend && nohup $MVN_CMD > ../backend.log 2>&1 &)
    gnome-terminal -- bash -c "cd '$PWD/holo-cockpit-frontend' && npm run dev; exec bash" 2>/dev/null \
      || xterm -e "cd '$PWD/holo-cockpit-frontend' && npm run dev; bash" 2>/dev/null \
      || (cd holo-cockpit-frontend && nohup npm run dev > ../frontend.log 2>&1 &)
    ;;
esac

echo ""
echo "================================================================"
echo "  ✅ 启动完成！"
echo ""
echo "  📊 数据大屏：    http://localhost:3000/"
echo "  🔐 管理后台登录：http://localhost:3000/#/login"
echo ""
echo "  默认账号："
echo "    管理员  admin / admin123"
echo "    商家    merchant / merchant123"
echo "================================================================"
