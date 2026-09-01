import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const microservicesRoot = path.join(root, 'microservices');
const services = ['identity', 'traffic', 'local', 'ai', 'community', 'ops'];
const failures = [];

const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8');
const exists = (relative) => fs.existsSync(path.join(root, relative));
const walk = (directory, extension) => {
  if (!fs.existsSync(directory)) return [];
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(directory, entry.name);
    return entry.isDirectory() ? walk(full, extension) : (full.endsWith(extension) ? [full] : []);
  });
};

const pomFiles = walk(microservicesRoot, 'pom.xml');
for (const pom of pomFiles) {
  const content = fs.readFileSync(pom, 'utf8');
  const label = path.relative(root, pom);
  if (/backend[\\/]src/i.test(content)) failures.push(`${label} 仍引用 backend/src`);
  if (content.includes('build-helper-maven-plugin')) failures.push(`${label} 仍使用 build-helper 注入单体源码`);
}

const allowedContractSources = new Set([
  'com/travelmate/common/AuthenticatedUser.java',
  'com/travelmate/common/Result.java',
  'com/travelmate/integration/CouponGateway.java',
  'com/travelmate/integration/NotificationGateway.java',
  'com/travelmate/integration/PassengerGateway.java'
]);
const contractRoot = path.join(microservicesRoot, 'travelmate-contract', 'src', 'main', 'java');
const contractSources = walk(contractRoot, '.java')
  .map((file) => path.relative(contractRoot, file).replaceAll('\\', '/'));
for (const required of allowedContractSources) {
  if (!contractSources.includes(required)) failures.push(`共享契约缺少 ${required}`);
}
for (const source of contractSources) {
  if (!allowedContractSources.has(source)) failures.push(`共享契约包含非契约业务代码 ${source}`);
}

const tableOwners = new Map();
for (const service of services) {
  const schema = read(`microservices/sql/${service}-schema.sql`);
  const tables = [...schema.matchAll(/CREATE\s+TABLE\s+IF\s+NOT\s+EXISTS\s+`([^`]+)`/gi)].map((match) => match[1]);
  if (tables.length === 0) failures.push(`${service}-schema.sql 未识别到业务表`);
  for (const table of tables) {
    if (tableOwners.has(table)) failures.push(`${table} 同时属于 ${tableOwners.get(table)} 和 ${service}`);
    tableOwners.set(table, service);
  }
}

const mapperOwners = new Map();
for (const service of services) {
  const javaRoot = path.join(microservicesRoot, 'services', `${service}-service`, 'src', 'main', 'java');
  for (const file of walk(javaRoot, '.java')) {
    if (path.basename(path.dirname(file)) === 'mapper') mapperOwners.set(path.basename(file, '.java'), service);
  }
}

for (const service of services) {
  const serviceRoot = path.join(microservicesRoot, 'services', `${service}-service`);
  const javaFiles = walk(path.join(serviceRoot, 'src', 'main', 'java'), '.java');
  if (javaFiles.length === 0) failures.push(`${service}-service 没有独立源码`);
  for (const file of javaFiles) {
    const content = fs.readFileSync(file, 'utf8');
    const label = path.relative(root, file);
    const referencedTables = new Set([...content.matchAll(/\btm_[a-z0-9_]+\b/gi)].map((match) => match[0]));
    for (const table of referencedTables) {
      const owner = tableOwners.get(table);
      if (owner && owner !== service) failures.push(`${label} 跨服务引用 ${owner} 的表 ${table}`);
    }
    const importedMappers = [...content.matchAll(/import\s+com\.travelmate\.mapper\.([A-Za-z0-9_]+);/g)]
      .map((match) => match[1]);
    for (const mapper of importedMappers) {
      const owner = mapperOwners.get(mapper);
      if (owner && owner !== service) failures.push(`${label} 跨服务引用 ${owner} 的 ${mapper}`);
    }
  }

  const application = read(`microservices/services/${service}-service/src/main/resources/application.yml`);
  if (!application.includes(`DB_NAME:travelmate_${service}`)) {
    failures.push(`${service}-service 默认数据库不是 travelmate_${service}`);
  }
}

const compose = read('microservices/compose.yml');
for (const service of services) {
  if (!compose.includes(`DB_NAME: travelmate_${service}`)) failures.push(`compose 缺少 ${service} 的独立 DB_NAME`);
  if (!compose.includes(`DB_USERNAME: travelmate_${service}_app`)) failures.push(`compose 缺少 ${service} 的独立数据库账号`);
}

const requiredBoundaryFiles = [
  'microservices/services/identity-service/src/main/java/com/travelmate/microservices/identity/InternalIdentityController.java',
  'microservices/services/local-service/src/main/java/com/travelmate/microservices/local/InternalCouponController.java',
  'microservices/services/traffic-service/src/main/java/com/travelmate/microservices/traffic/TrafficIntegrationGateway.java',
  'microservices/services/traffic-service/src/main/java/com/travelmate/microservices/traffic/TrafficOutboxNotificationGateway.java',
  'microservices/services/local-service/src/main/java/com/travelmate/microservices/local/LocalOutboxNotificationGateway.java',
  'microservices/services/ai-service/src/main/java/com/travelmate/microservices/ai/InternalNotificationEventController.java',
  'microservices/services/identity-service/src/main/java/com/travelmate/microservices/identity/InternalCommunityIdentityController.java',
  'microservices/services/community-service/src/main/java/com/travelmate/microservices/community/InternalAdminCommunityController.java',
  'microservices/services/local-service/src/main/java/com/travelmate/microservices/local/InternalAdminLocalController.java',
  'microservices/services/traffic-service/src/main/java/com/travelmate/microservices/traffic/InternalAdminTrafficController.java',
  'microservices/services/ops-service/src/main/java/com/travelmate/microservices/ops/InternalContentSafetyController.java'
];
for (const required of requiredBoundaryFiles) {
  if (!exists(required)) failures.push(`缺少跨服务边界实现 ${required}`);
}

if (failures.length > 0) {
  console.error('Microservice boundary check failed:');
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log(`Microservice boundary check passed: ${services.length} services, ${tableOwners.size} uniquely owned tables.`);
