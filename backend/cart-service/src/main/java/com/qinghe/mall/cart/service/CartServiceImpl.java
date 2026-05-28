package com.qinghe.mall.cart.service;

import com.qinghe.mall.cart.client.GoodsClient;
import com.qinghe.mall.cart.dto.CartAddDTO;
import com.qinghe.mall.cart.dto.CartItemVO;
import com.qinghe.mall.cart.dto.CartUpdateDTO;
import com.qinghe.mall.cart.entity.Cart;
import com.qinghe.mall.cart.mapper.CartMapper;
import com.qinghe.mall.common.R;
import com.qinghe.mall.common.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 购物车服务实现
 */
@Service
public class CartServiceImpl implements CartService {

    private static final String REDIS_CART_PREFIX = "cart:";
    private static final long REDIS_CART_EXPIRE_DAYS = 30;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void addCart(Long userId, CartAddDTO cartAddDTO) {
        // 远程调用校验商品
        R<Map<String, Object>> goodsResult = goodsClient.getGoods(cartAddDTO.getGoodsId());
        if (goodsResult == null || goodsResult.getCode() != StatusCode.SUCCESS) {
            throw new RuntimeException("商品信息获取失败");
        }

        Map<String, Object> goodsInfo = goodsResult.getData();
        if (goodsInfo == null) {
            throw new RuntimeException("商品不存在");
        }

        // 校验库存
        Integer stock = (Integer) goodsInfo.get("stock");
        if (stock == null || stock < cartAddDTO.getQuantity()) {
            throw new RuntimeException("库存不足");
        }

        // 构建购物车项
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setGoodsId(cartAddDTO.getGoodsId());
        cart.setGoodsName((String) goodsInfo.get("name"));
        cart.setGoodsImage((String) goodsInfo.get("image"));
        cart.setPrice(new BigDecimal(goodsInfo.get("price").toString()));
        cart.setQuantity(cartAddDTO.getQuantity());
        cart.setChecked(1);

        // 检查是否已存在该商品在购物车中
        Cart existCart = cartMapper.findByUserIdAndGoodsId(userId, cartAddDTO.getGoodsId());
        if (existCart != null) {
            // 更新数量
            cartMapper.updateQuantity(existCart.getId(), existCart.getQuantity() + cartAddDTO.getQuantity());
        } else {
            // 新增
            cartMapper.insert(cart);
        }
    }

    @Override
    public List<CartItemVO> getCartList(Long userId) {
        List<Cart> cartList = cartMapper.findByUserId(userId);
        if (cartList == null || cartList.isEmpty()) {
            return new ArrayList<>();
        }

        // 远程调用获取最新商品信息
        String goodsIds = cartList.stream()
                .map(c -> c.getGoodsId().toString())
                .collect(Collectors.joining(","));

        R<Map<Long, Map<String, Object>>> batchResult = goodsClient.getGoodsBatch(goodsIds);
        Map<Long, Map<String, Object>> goodsMap = null;
        if (batchResult != null && batchResult.getCode() == StatusCode.SUCCESS) {
            goodsMap = batchResult.getData();
        }

        // 转换为VO
        List<CartItemVO> voList = new ArrayList<>();
        for (Cart cart : cartList) {
            CartItemVO vo = new CartItemVO();
            vo.setId(cart.getId());
            vo.setGoodsId(cart.getGoodsId());
            vo.setSkuId(cart.getSkuId());
            vo.setQuantity(cart.getQuantity());
            vo.setChecked(cart.getChecked());

            // 更新最新价格
            if (goodsMap != null && goodsMap.containsKey(cart.getGoodsId())) {
                Map<String, Object> goodsInfo = goodsMap.get(cart.getGoodsId());
                vo.setGoodsName((String) goodsInfo.get("name"));
                vo.setGoodsImage((String) goodsInfo.get("image"));
                vo.setPrice(new BigDecimal(goodsInfo.get("price").toString()));
            } else {
                vo.setGoodsName(cart.getGoodsName());
                vo.setGoodsImage(cart.getGoodsImage());
                vo.setPrice(cart.getPrice());
            }

            // 计算小计
            vo.setSubtotal(vo.getPrice().multiply(new BigDecimal(vo.getQuantity())));
            voList.add(vo);
        }

        return voList;
    }

    @Override
    public void updateQuantity(Long userId, CartUpdateDTO cartUpdateDTO) {
        // 校验购物车项是否属于该用户
        List<Cart> cartList = cartMapper.findByUserId(userId);
        boolean owned = cartList.stream()
                .anyMatch(c -> c.getId().equals(cartUpdateDTO.getCartId()));
        if (!owned) {
            throw new RuntimeException("购物车项不存在");
        }

        // 校验库存
        Cart cart = cartList.stream()
                .filter(c -> c.getId().equals(cartUpdateDTO.getCartId()))
                .findFirst()
                .orElse(null);

        if (cart != null) {
            R<Map<String, Object>> goodsResult = goodsClient.getGoods(cart.getGoodsId());
            if (goodsResult != null && goodsResult.getCode() == StatusCode.SUCCESS) {
                Map<String, Object> goodsInfo = goodsResult.getData();
                Integer stock = (Integer) goodsInfo.get("stock");
                if (stock == null || stock < cartUpdateDTO.getQuantity()) {
                    throw new RuntimeException("库存不足");
                }
            }
        }

        cartMapper.updateQuantity(cartUpdateDTO.getCartId(), cartUpdateDTO.getQuantity());
    }

    @Override
    public void deleteCart(Long userId, Long cartId) {
        // 校验购物车项是否属于该用户
        List<Cart> cartList = cartMapper.findByUserId(userId);
        boolean owned = cartList.stream()
                .anyMatch(c -> c.getId().equals(cartId));
        if (!owned) {
            throw new RuntimeException("购物车项不存在");
        }

        cartMapper.deleteById(cartId);
    }

    @Override
    public void toggleChecked(Long userId, Long cartId) {
        // 校验购物车项是否属于该用户
        List<Cart> cartList = cartMapper.findByUserId(userId);
        Cart cart = cartList.stream()
                .filter(c -> c.getId().equals(cartId))
                .findFirst()
                .orElse(null);

        if (cart == null) {
            throw new RuntimeException("购物车项不存在");
        }

        // 切换选中状态
        int newChecked = cart.getChecked() == 1 ? 0 : 1;
        cartMapper.updateChecked(cartId, newChecked);
    }

    @Override
    public void selectAll(Long userId, Boolean checked) {
        int checkedValue = Boolean.TRUE.equals(checked) ? 1 : 0;
        cartMapper.updateCheckedByUserId(userId, checkedValue);
    }
}
