package cn.ecom.service;

import cn.ecom.domain.pojo.Address;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AddressService extends IService<Address> {

    /** 当前用户的地址列表（默认地址在前） */
    List<Address> listMine();

    void addAddress(Address address);

    void updateAddress(Address address);

    void deleteAddress(Long id);

    /** 设为默认地址（同时取消其他默认） */
    void setDefault(Long id);
}
