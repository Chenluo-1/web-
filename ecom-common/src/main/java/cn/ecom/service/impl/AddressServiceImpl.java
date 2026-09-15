package cn.ecom.service.impl;

import cn.ecom.domain.pojo.Address;
import cn.ecom.handler.exception.BusinessException;
import cn.ecom.mapper.AddressMapper;
import cn.ecom.service.AddressService;
import cn.ecom.utils.AppHttpCodeEnum;
import cn.ecom.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 收货地址服务实现（全部按当前用户隔离）
 */
@Service("addressService")
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    @Override
    public List<Address> listMine() {
        return list(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, SecurityUtils.getUserId())
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getId));
    }

    @Override
    @Transactional
    public void addAddress(Address address) {
        validate(address);
        address.setId(null);
        address.setUserId(SecurityUtils.getUserId());
        if (address.getIsDefault() == null) {
            address.setIsDefault(listMine().isEmpty() ? 1 : 0);
        }
        if (address.getIsDefault() == 1) {
            clearDefault();
        }
        save(address);
    }

    @Override
    @Transactional
    public void updateAddress(Address address) {
        if (address.getId() == null) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "缺少地址id");
        }
        validate(address);
        Address owned = getOwned(address.getId());
        address.setUserId(owned.getUserId());
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault();
        }
        updateById(address);
    }

    @Override
    public void deleteAddress(Long id) {
        getOwned(id);
        removeById(id);
    }

    @Override
    @Transactional
    public void setDefault(Long id) {
        Address owned = getOwned(id);
        clearDefault();
        updateById(new Address().setId(owned.getId()).setIsDefault(1));
    }

    private void clearDefault() {
        List<Address> mine = listMine();
        for (Address address : mine) {
            if (address.getIsDefault() != null && address.getIsDefault() == 1) {
                updateById(new Address().setId(address.getId()).setIsDefault(0));
            }
        }
    }

    private void validate(Address address) {
        if (!StringUtils.hasText(address.getReceiverName())) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "收货人不能为空");
        }
        if (!StringUtils.hasText(address.getPhone())) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "联系电话不能为空");
        }
        if (!StringUtils.hasText(address.getDetail())) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "详细地址不能为空");
        }
    }

    private Address getOwned(Long id) {
        Address address = getById(id);
        if (address == null || !address.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "地址不存在");
        }
        return address;
    }
}
