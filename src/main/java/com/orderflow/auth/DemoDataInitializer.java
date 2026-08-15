package com.orderflow.auth;

import com.orderflow.domain.entity.Category;
import com.orderflow.domain.entity.Customer;
import com.orderflow.domain.entity.Inventory;
import com.orderflow.domain.entity.Product;
import com.orderflow.domain.entity.Promotion;
import com.orderflow.domain.entity.Store;
import com.orderflow.domain.entity.Tenant;
import com.orderflow.domain.mapper.CategoryMapper;
import com.orderflow.domain.mapper.CustomerMapper;
import com.orderflow.domain.mapper.InventoryMapper;
import com.orderflow.domain.mapper.ProductMapper;
import com.orderflow.domain.mapper.PromotionMapper;
import com.orderflow.domain.mapper.StoreMapper;
import com.orderflow.domain.mapper.TenantMapper;
import com.orderflow.order.CreateOrderRequest;
import com.orderflow.order.OrderService;
import com.orderflow.refund.RefundService;
import com.orderflow.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 演示数据初始化（真实业务场景版）：为演示租户 t-a（admin-a）灌入一套接近真实电商经营的演示数据——
 * 多级分类、多门店、带分类/门店维度的商品与库存、多档营销活动，以及最近 30 天内分布合理、
 * 状态齐全（待支付/已确认/已发货/已完成/退款中/已退款/已取消）、部分使用促销、含真实退款流程的订单。
 *
 * 订单通过 {@link OrderService} 真实业务流创建（保证库存预占、订单项、状态历史一致），
 * 之后回写 created_at 以铺开到最近 30 天，使仪表盘「近 7 天趋势」「今日订单」等指标开箱即有真实形态。
 *
 * 幂等：以商品表是否已有该租户数据作为开关，可重复启动不重复灌入。
 */
@Component
@Order(2)
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final String TENANT_CODE = "t-a";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TenantMapper tenantMapper;
    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final CategoryMapper categoryMapper;
    private final StoreMapper storeMapper;
    private final PromotionMapper promotionMapper;
    private final OrderService orderService;
    private final RefundService refundService;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DemoDataInitializer(TenantMapper tenantMapper, ProductMapper productMapper,
                               InventoryMapper inventoryMapper, CategoryMapper categoryMapper,
                               StoreMapper storeMapper, PromotionMapper promotionMapper,
                               OrderService orderService, RefundService refundService,
                               CustomerMapper customerMapper, PasswordEncoder passwordEncoder,
                               JdbcTemplate jdbcTemplate) {
        this.tenantMapper = tenantMapper;
        this.productMapper = productMapper;
        this.inventoryMapper = inventoryMapper;
        this.categoryMapper = categoryMapper;
        this.storeMapper = storeMapper;
        this.promotionMapper = promotionMapper;
        this.orderService = orderService;
        this.refundService = refundService;
        this.customerMapper = customerMapper;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        Tenant tenant = tenantMapper.selectOne(new QueryWrapper<Tenant>().eq("tenant_code", TENANT_CODE));
        if (tenant == null) {
            log.warn("未找到演示租户 {}，跳过演示数据初始化", TENANT_CODE);
            return;
        }
        Long tenantId = tenant.getId();
        TenantContext.set(tenantId, 1L, "seed");
        try {
            long prodCount = productMapper.selectCount(new QueryWrapper<Product>().eq("tenant_id", tenantId));
            long custCount = customerMapper.selectCount(new QueryWrapper<Customer>().eq("tenant_id", tenantId));

            if (prodCount == 0) {
                Map<String, Long> catIds = seedCategories(tenantId);
                Map<String, Long> storeIds = seedStores(tenantId);
                Map<String, Long> prodIds = seedCatalog(tenantId, catIds, storeIds);
                Map<String, String> promos = seedPromotions(tenantId);
                seedOrders(tenantId, prodIds, promos);
            } else {
                log.info("演示商品已存在（商品数={}），跳过商品/订单初始化", prodCount);
            }

            if (custCount == 0) {
                seedCustomers(tenantId);
            } else {
                log.info("演示顾客已存在（顾客数={}），跳过顾客初始化", custCount);
            }
            log.info("演示数据初始化完成");
        } finally {
            TenantContext.clear();
        }

        // 给 t-b 也灌一份"另一家商家"的门店 + 商品，演示"商品由不同租户上传"
        seedOtherTenantIfNeeded("t-b");
    }

    /**
     * 给另一个演示租户种独立门店 + 商品。独立上下文，避免被 t-a 的租户上下文污染。
     * 这样顾客端就能跨租户浏览，商品详情页"所属门店"显示的是真实商家门店名。
     */
    private void seedOtherTenantIfNeeded(String tenantCode) {
        Tenant tenantB = tenantMapper.selectOne(new QueryWrapper<Tenant>().eq("tenant_code", tenantCode));
        if (tenantB == null) return;
        Long tenantIdB = tenantB.getId();
        // 先设租户上下文，否则 productMapper.selectCount 会被多租户拦截器拒绝
        TenantContext.set(tenantIdB, 1L, "seed");
        try {
            long prodCountB = productMapper.selectCount(new QueryWrapper<Product>().eq("tenant_id", tenantIdB));
            if (prodCountB > 0) {
                log.info("租户 {} 演示商品已存在（商品数={}），跳过", tenantCode, prodCountB);
                return;
            }
            seedOtherTenant(tenantIdB);
        } finally {
            TenantContext.clear();
        }
    }

    private void seedOtherTenant(Long tenantId) {
        // 独立分类（每个租户有自己的分类树）
        Map<String, Long> catIds = seedCategories(tenantId);

        // 独立门店（专属门店名以示区别）
        Store st = new Store();
        st.setTenantId(tenantId);
        st.setStoreCode("ST-BJ-B");
        st.setStoreName("广州旗舰店");
        st.setProvince("广东省");
        st.setCity("广州市");
        st.setAddress("番禺区长隆大道 88 号");
        st.setStatus(1);
        storeMapper.insert(st);
        Long storeId = st.getId();

        // 独立商品（SKU 前缀区分租户，类目与 t-a 互补以展示真实"另一家"）
        List<ProdSeed> seeds = List.of(
                new ProdSeed("SKU-B-001", "智能手表运动版",    catIds.get("CAT-DIGITAL"), storeId, 89900L, 50, 8),
                new ProdSeed("SKU-B-002", "无线入耳式耳机",    catIds.get("CAT-AV"),      storeId, 29900L, 180, 30),
                new ProdSeed("SKU-B-003", "Type-C 快充线",      catIds.get("CAT-ACC"),     storeId, 4900L, 400, 80),
                new ProdSeed("SKU-B-004", "人体工学椅",          catIds.get("CAT-OFFICE"),  storeId, 129900L, 15, 3),
                new ProdSeed("SKU-B-005", "智能体重秤",          catIds.get("CAT-SMART"),   storeId, 14900L, 200, 25),
                new ProdSeed("SKU-B-006", "桌面摄像头",          catIds.get("CAT-OFFICE"),  storeId, 39900L, 70, 12),
                new ProdSeed("SKU-B-007", "便携投影仪",          catIds.get("CAT-AV"),      storeId, 199900L, 20, 4),
                new ProdSeed("SKU-B-008", "智能音箱",            catIds.get("CAT-SMART"),   storeId, 34900L, 120, 18));
        for (ProdSeed s : seeds) {
            Product p = new Product();
            p.setTenantId(tenantId);
            p.setProductCode(s.code());
            p.setProductName(s.name());
            p.setCategoryId(s.categoryId());
            p.setStoreId(s.storeId());
            p.setUnitPriceCent(s.price());
            p.setStatus(1);
            p.setSales(1000L + 400000L / (s.physical() + 20L));
            productMapper.insert(p);

            Inventory inv = new Inventory();
            inv.setTenantId(tenantId);
            inv.setProductId(p.getId());
            inv.setPhysicalQuantity((long) s.physical());
            inv.setReservedQuantity(0L);
            inv.setVersion(0L);
            inventoryMapper.insert(inv);
        }
        log.info("租户 {} 已初始化 1 个门店 + {} 个商品（广州旗舰店）", tenantId, seeds.size());
    }

    // ---------- 分类 ----------
    private Map<String, Long> seedCategories(Long tenantId) {
        List<CatSeed> seeds = List.of(
                new CatSeed("CAT-DIGITAL", "数码电子"),
                new CatSeed("CAT-ACC", "手机配件"),
                new CatSeed("CAT-OFFICE", "办公设备"),
                new CatSeed("CAT-SMART", "智能家居"),
                new CatSeed("CAT-AV", "影音娱乐"));
        for (CatSeed s : seeds) {
            Category c = new Category();
            c.setTenantId(tenantId);
            c.setCategoryCode(s.code());
            c.setCategoryName(s.name());
            c.setParentId(0L);
            c.setSort(0);
            c.setStatus(1);
            categoryMapper.insert(c);
        }
        return categoryMapper.selectList(new QueryWrapper<Category>().eq("tenant_id", tenantId))
                .stream().collect(Collectors.toMap(Category::getCategoryCode, Category::getId));
    }

    // ---------- 门店 ----------
    private Map<String, Long> seedStores(Long tenantId) {
        List<StoreSeed> seeds = List.of(
                new StoreSeed("ST-BJ", "北京旗舰店", "北京市", "北京市", "朝阳区建国路88号"),
                new StoreSeed("ST-SH", "上海门店", "上海市", "上海市", "浦东新区世纪大道100号"),
                new StoreSeed("ST-GZ", "广州门店", "广东省", "广州市", "天河区天河路200号"));
        for (StoreSeed s : seeds) {
            Store st = new Store();
            st.setTenantId(tenantId);
            st.setStoreCode(s.code());
            st.setStoreName(s.name());
            st.setProvince(s.province());
            st.setCity(s.city());
            st.setAddress(s.address());
            st.setStatus(1);
            storeMapper.insert(st);
        }
        return storeMapper.selectList(new QueryWrapper<Store>().eq("tenant_id", tenantId))
                .stream().collect(Collectors.toMap(Store::getStoreCode, Store::getId));
    }

    // ---------- 商品 + 库存 ----------
    private Map<String, Long> seedCatalog(Long tenantId, Map<String, Long> catIds, Map<String, Long> storeIds) {
        List<ProdSeed> seeds = List.of(
                new ProdSeed("SKU-A-001", "人体工学无线鼠标", catIds.get("CAT-ACC"), storeIds.get("ST-BJ"), 9900L, 120, 30),
                new ProdSeed("SKU-A-002", "客制化机械键盘", catIds.get("CAT-ACC"), storeIds.get("ST-BJ"), 39900L, 60, 12),
                new ProdSeed("SKU-A-003", "27寸 4K 显示器", catIds.get("CAT-DIGITAL"), storeIds.get("ST-BJ"), 129900L, 35, 8),
                new ProdSeed("SKU-A-004", "USB-C 九合一扩展坞", catIds.get("CAT-ACC"), storeIds.get("ST-SH"), 3900L, 200, 45),
                new ProdSeed("SKU-A-005", "主动降噪头戴耳机", catIds.get("CAT-AV"), storeIds.get("ST-SH"), 89900L, 50, 7),
                new ProdSeed("SKU-A-006", "铝合金笔记本支架", catIds.get("CAT-OFFICE"), storeIds.get("ST-BJ"), 12900L, 90, 25),
                new ProdSeed("SKU-A-007", "高速 NVMe 1TB 固态硬盘", catIds.get("CAT-DIGITAL"), storeIds.get("ST-GZ"), 54900L, 70, 15),
                new ProdSeed("SKU-A-008", "便携蓝牙音箱", catIds.get("CAT-AV"), storeIds.get("ST-GZ"), 29900L, 110, 18),
                new ProdSeed("SKU-A-009", "智能扫地机器人", catIds.get("CAT-SMART"), storeIds.get("ST-BJ"), 199900L, 25, 4),
                new ProdSeed("SKU-A-010", "智能门锁指纹版", catIds.get("CAT-SMART"), storeIds.get("ST-SH"), 159900L, 40, 6),
                new ProdSeed("SKU-A-011", "65W 氮化镓充电器", catIds.get("CAT-ACC"), storeIds.get("ST-GZ"), 14900L, 300, 60),
                new ProdSeed("SKU-A-012", "千兆路由 Mesh 套装", catIds.get("CAT-SMART"), storeIds.get("ST-GZ"), 69900L, 55, 10),
                new ProdSeed("SKU-A-013", "4K 投影仪", catIds.get("CAT-AV"), storeIds.get("ST-BJ"), 349900L, 18, 3),
                new ProdSeed("SKU-A-014", "激光无线打印机", catIds.get("CAT-OFFICE"), storeIds.get("ST-SH"), 119900L, 22, 5),
                new ProdSeed("SKU-A-015", "电竞显示器 240Hz", catIds.get("CAT-DIGITAL"), storeIds.get("ST-SH"), 229900L, 30, 9),
                new ProdSeed("SKU-A-016", "降噪通话商务耳机", catIds.get("CAT-AV"), storeIds.get("ST-GZ"), 45900L, 80, 14),
                new ProdSeed("SKU-A-017", "无线充电底座", catIds.get("CAT-ACC"), storeIds.get("ST-BJ"), 8900L, 260, 50),
                new ProdSeed("SKU-A-018", "智能体脂秤", catIds.get("CAT-SMART"), storeIds.get("ST-SH"), 19900L, 140, 22),
                new ProdSeed("SKU-A-019", "高速读卡器", catIds.get("CAT-ACC"), storeIds.get("ST-GZ"), 5900L, 400, 80),
                new ProdSeed("SKU-A-020", "桌面无线充台灯", catIds.get("CAT-OFFICE"), storeIds.get("ST-BJ"), 25900L, 95, 16),
                new ProdSeed("SKU-A-021", "便携 SSD 2TB", catIds.get("CAT-DIGITAL"), storeIds.get("ST-GZ"), 99900L, 48, 11),
                new ProdSeed("SKU-A-022", "机械臂手机支架", catIds.get("CAT-ACC"), storeIds.get("ST-SH"), 6900L, 220, 40),
                new ProdSeed("SKU-A-023", "智能空气净化器", catIds.get("CAT-SMART"), storeIds.get("ST-BJ"), 179900L, 28, 5),
                new ProdSeed("SKU-A-024", "会议全向麦克风", catIds.get("CAT-OFFICE"), storeIds.get("ST-GZ"), 79900L, 36, 8));
        Map<String, Long> idMap = new java.util.LinkedHashMap<>();
        for (ProdSeed s : seeds) {
            Product p = new Product();
            p.setTenantId(tenantId);
            p.setProductCode(s.code());
            p.setProductName(s.name());
            p.setCategoryId(s.categoryId());
            p.setStoreId(s.storeId());
            p.setUnitPriceCent(s.price());
            p.setStatus(1);
            // 销量模拟：库存越少的越畅销，量级 1k~1.6w，驱动商城“已售/热卖”标签
            p.setSales(1000L + 400000L / (s.physical() + 20L));
            productMapper.insert(p);
            idMap.put(s.code(), p.getId());

            Inventory inv = new Inventory();
            inv.setTenantId(tenantId);
            inv.setProductId(p.getId());
            inv.setPhysicalQuantity((long) s.physical());
            inv.setReservedQuantity(0L);
            inv.setVersion(0L);
            inventoryMapper.insert(inv);
        }
        log.info("已初始化 {} 个商品与库存（含分类与门店维度）", seeds.size());
        return idMap;
    }

    // ---------- 营销活动 ----------
    private Map<String, String> seedPromotions(Long tenantId) {
        LocalDateTime begin = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2027, 12, 31, 23, 59);
        List<PromoSeed> seeds = List.of(
                new PromoSeed("FULL200-30", "满200减30", "FULL_REDUCTION", 20000L, 3000L),
                new PromoSeed("FULL500-80", "满500减80", "FULL_REDUCTION", 50000L, 8000L),
                new PromoSeed("NEWUSER-20", "新人首单立减20", "INSTANT_REDUCTION", 0L, 2000L));
        for (PromoSeed s : seeds) {
            Promotion p = new Promotion();
            p.setTenantId(tenantId);
            p.setPromoCode(s.code());
            p.setPromoName(s.name());
            p.setPromoType(s.type());
            p.setThresholdCent(s.threshold());
            p.setDiscountAmountCent(s.discount());
            p.setBeginAt(begin);
            p.setEndAt(end);
            p.setStatus(1);
            promotionMapper.insert(p);
        }
        log.info("已初始化 {} 个营销活动（满减/新人券）", seeds.size());
        return seeds.stream().collect(Collectors.toMap(PromoSeed::code, PromoSeed::code));
    }

    // ---------- 历史订单（真实业务流 + 回写时间） ----------
    private void seedOrders(Long tenantId, Map<String, Long> prodIds, Map<String, String> promos) {
        Random rnd = new Random(20260815L);
        List<String> allSkus = new ArrayList<>(prodIds.keySet());
        List<String> customers = List.of(
                "张伟", "王芳", "李娜", "刘洋", "陈静", "杨帆", "赵磊", "黄敏", "周强", "吴婷",
                "徐杰", "孙丽", "马超", "朱琳", "胡军", "郭涛", "林峰", "何雪", "高翔", "罗静",
                "梁宇", "宋佳", "唐磊", "韩雪", "冯刚", "邓超", "曹颖", "彭勇", "曾敏", "蒋勤");

        // 各状态的目标占比（合计 100）
        List<OrderScenario> scenarios = new ArrayList<>();
        for (int i = 0; i < 6; i++) scenarios.add(new OrderScenario("COMPLETED", false));
        for (int i = 0; i < 4; i++) scenarios.add(new OrderScenario("SHIPPED", false));
        for (int i = 0; i < 3; i++) scenarios.add(new OrderScenario("CONFIRMED", false));
        for (int i = 0; i < 2; i++) scenarios.add(new OrderScenario("CREATED", false));
        for (int i = 0; i < 2; i++) scenarios.add(new OrderScenario("CANCELLED", false));
        for (int i = 0; i < 2; i++) scenarios.add(new OrderScenario("REFUNDING", false));
        for (int i = 0; i < 5; i++) scenarios.add(new OrderScenario("REFUNDED", true));

        int n = scenarios.size();
        LocalDate today = LocalDate.now();
        int idx = 0;
        for (OrderScenario sc : scenarios) {
            // 1~2 个商品行，数量 1~3
            int lines = 1 + rnd.nextInt(2);
            List<OrderItemSpec> items = new ArrayList<>();
            for (int k = 0; k < lines; k++) {
                String sku = allSkus.get(rnd.nextInt(allSkus.size()));
                items.add(new OrderItemSpec(sku, 1 + rnd.nextInt(3)));
            }
            // 约 40% 使用促销：小额用满200减30，中额用满500减80，新客用新人券
            String promo = null;
            double roll = rnd.nextDouble();
            if (roll < 0.4) {
                long amount = items.stream().mapToLong(it -> priceOf(prodIds, it) * it.qty).sum();
                if (amount >= 50000) promo = "FULL500-80";
                else if (amount >= 20000) promo = "FULL200-30";
                else promo = "NEWUSER-20";
            }

            CreateOrderRequest req = new CreateOrderRequest();
            req.setCustomerName(customers.get(rnd.nextInt(customers.size())));
            if (promo != null) req.setPromoCode(promo);
            req.setItems(items.stream().map(it -> item(prodIds, it)).toList());

            String idem = "seed-order-" + idx;
            Long orderId = orderService.create(req, idem).getId();

            switch (sc.status) {
                case "CONFIRMED" -> orderService.confirm(orderId);
                case "SHIPPED" -> { orderService.confirm(orderId); orderService.ship(orderId); }
                case "COMPLETED" -> { orderService.confirm(orderId); orderService.ship(orderId); orderService.complete(orderId); }
                case "CANCELLED" -> { orderService.confirm(orderId); orderService.cancel(orderId); }
                case "REFUNDING" -> { orderService.confirm(orderId); orderService.ship(orderId); refundService.apply(orderId, "演示退款：收到货后发现外观瑕疵，申请退款"); }
                case "REFUNDED" -> { orderService.confirm(orderId); orderService.ship(orderId); var rf = refundService.apply(orderId, "演示退款：七天无理由退货"); refundService.approve(rf.getId()); }
                default -> { /* CREATED 保持待支付 */ }
            }

            // 回写创建时间：按状态铺开到合理的业务时点（待支付/已确认多为当天，已完成/已退款偏历史）
            int dayOffset = switch (sc.status) {
                case "CREATED" -> 0;
                case "CONFIRMED" -> rnd.nextInt(2);
                case "SHIPPED" -> 1 + rnd.nextInt(3);
                case "CANCELLED" -> 2 + rnd.nextInt(13);
                case "REFUNDING" -> 3 + rnd.nextInt(8);
                case "REFUNDED" -> 8 + rnd.nextInt(21);
                default -> 5 + rnd.nextInt(24); // COMPLETED
            };
            LocalDateTime created = today.minusDays(dayOffset)
                    .atTime(9 + rnd.nextInt(11), rnd.nextInt(60), rnd.nextInt(60));
            String ts = created.format(FMT);
            jdbcTemplate.update("UPDATE orders SET created_at = ?, updated_at = ? WHERE id = ?", ts, ts, orderId);

            idx++;
        }
        log.info("已通过真实业务流创建 {} 笔历史订单（覆盖 7 种状态，约 40% 使用促销，部分走退款流程）", n);
    }

    private long priceOf(Map<String, Long> prodIds, OrderItemSpec it) {
        Product p = productMapper.selectById(prodIds.get(it.sku));
        return p == null ? 0L : (p.getUnitPriceCent() == null ? 0L : p.getUnitPriceCent());
    }

    private CreateOrderRequest.OrderItemRequest item(Map<String, Long> prodIds, OrderItemSpec spec) {
        CreateOrderRequest.OrderItemRequest it = new CreateOrderRequest.OrderItemRequest();
        it.setProductId(prodIds.get(spec.sku));
        it.setQuantity(spec.qty);
        return it;
    }

    private record CatSeed(String code, String name) {}
    private record StoreSeed(String code, String name, String province, String city, String address) {}
    private record ProdSeed(String code, String name, Long categoryId, Long storeId, Long price, int physical, int reserved) {}
    private record PromoSeed(String code, String name, String type, Long threshold, Long discount) {}
    private record OrderItemSpec(String sku, int qty) {}
    private record OrderScenario(String status, boolean refunded) {}

    // ---------- 演示顾客 + 顾客订单 ----------
    private void seedCustomers(Long tenantId) {
        List<CustSeed> seeds = List.of(
                new CustSeed("customer01", "体验顾客·小美", "13800000001"),
                new CustSeed("customer02", "体验顾客·阿强", "13800000002"),
                new CustSeed("customer03", "体验顾客·莉莉", "13800000003"));
        Map<String, Long> custIds = new java.util.LinkedHashMap<>();
        for (CustSeed s : seeds) {
            Customer c = new Customer();
            c.setTenantId(tenantId);
            c.setUsername(s.username());
            c.setPasswordHash(passwordEncoder.encode("admin123"));
            c.setNickname(s.nickname());
            c.setPhone(s.phone());
            c.setStatus(1);
            customerMapper.insert(c);
            custIds.put(s.username(), c.getId());
        }
        log.info("已初始化 {} 个演示顾客账号（密码均为 admin123）", seeds.size());

        // 给每位顾客生成 1~2 笔订单，复用真实下单链路（含库存预占、营销减免、状态机）
        Map<String, Long> prodIds = productMapper.selectList(new QueryWrapper<Product>().eq("tenant_id", tenantId))
                .stream().collect(Collectors.toMap(Product::getProductCode, Product::getId));
        List<String> allSkus = new ArrayList<>(prodIds.keySet());
        Random rnd = new Random(20260815L);
        LocalDate today = LocalDate.now();
        int seq = 0;
        for (var entry : custIds.entrySet()) {
            String username = entry.getKey();
            Long cid = entry.getValue();
            int nOrders = 1 + rnd.nextInt(2);
            for (int k = 0; k < nOrders; k++) {
                int lines = 1 + rnd.nextInt(2);
                CreateOrderRequest req = new CreateOrderRequest();
                req.setCustomerName(username);
                req.setCustomerId(cid);
                List<CreateOrderRequest.OrderItemRequest> items = new ArrayList<>();
                for (int m = 0; m < lines; m++) {
                    String sku = allSkus.get(rnd.nextInt(allSkus.size()));
                    CreateOrderRequest.OrderItemRequest it = new CreateOrderRequest.OrderItemRequest();
                    it.setProductId(prodIds.get(sku));
                    it.setQuantity(1 + rnd.nextInt(2));
                    items.add(it);
                }
                req.setItems(items);
                Long oid = orderService.create(req, "seed-cust-" + cid + "-" + k).getId();
                orderService.confirm(oid);
                orderService.ship(oid);
                orderService.complete(oid);

                int dayOffset = 1 + rnd.nextInt(20);
                LocalDateTime created = today.minusDays(dayOffset).atTime(10 + rnd.nextInt(10), rnd.nextInt(60), rnd.nextInt(60));
                String ts = created.format(FMT);
                jdbcTemplate.update("UPDATE orders SET created_at = ?, updated_at = ? WHERE id = ?", ts, ts, oid);
                seq++;
            }
        }
        log.info("已为演示顾客生成 {} 笔已完成订单（顾客端「我的订单」可直接查看）", seq);
    }

    private record CustSeed(String username, String nickname, String phone) {}
}
