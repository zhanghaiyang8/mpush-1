package com.shinemo.mpush.core.router;

import com.google.common.eventbus.Subscribe;
import com.shinemo.mpush.api.event.ConnectionCloseEvent;
import com.shinemo.mpush.api.router.RouterManager;
import com.shinemo.mpush.common.EventBus;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by ohun on 2015/12/23.
 */
public final class LocalRouterManager implements RouterManager<LocalRouter> {
    public static final Logger LOGGER = LoggerFactory.getLogger(LocalRouterManager.class);

    /**
     * 本地路由表
     */
    private final Map<String, LocalRouter> routers = new ConcurrentHashMapV8<>();

    /**
     * 反向关系表
     */
    private final Map<String, String> connIdUserIds = new ConcurrentHashMapV8<>();

    public LocalRouterManager() {
        EventBus.INSTANCE.register(this);
    }

    @Override
    public LocalRouter register(String userId, LocalRouter router) {
        LOGGER.debug("register local router success userId={}, router={}", userId, router);
        connIdUserIds.put(router.getRouteValue().getId(), userId);
        return routers.put(userId, router);
    }

    @Override
    public boolean unRegister(String userId) {
        LocalRouter router = routers.remove(userId);
        if (router != null) {
            connIdUserIds.remove(router.getRouteValue().getId());
        }
        LOGGER.info("unRegister local router success userId={}, router={}", userId, router);
        return true;
    }

    @Override
    public LocalRouter lookup(String userId) {
        LocalRouter router = routers.get(userId);
        LOGGER.debug("lookup local router userId={}, router={}", userId, router);
        return router;
    }

    /**
     * 监听链接关闭事件，清理失效的路由
     *
     * @param event
     */
    @Subscribe
    void onConnectionCloseEvent(ConnectionCloseEvent event) {
        String id = event.connection.getId();

        //1.清除反向关系
        String userId = connIdUserIds.remove(id);
        if (userId == null) return;

        LocalRouter router = routers.get(userId);
        if (router == null) return;

        //2.检测下，是否是同一个链接, 如果客户端重连，老的路由会被新的链接覆盖
        if (id.equals(router.getRouteValue().getId())) {

            //3.删除路由
            routers.remove(userId);
            LOGGER.warn("clean disconnected local route, userId={}, route={}", userId, router);
        }
    }
}