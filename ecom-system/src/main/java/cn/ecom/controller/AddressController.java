package cn.ecom.controller;

import cn.ecom.domain.pojo.Address;
import cn.ecom.service.AddressService;
import cn.ecom.utils.ResponseResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收货地址（消费者）
 */
@RestController
@RequestMapping("/address")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    /** 我的地址列表 */
    @GetMapping
    public ResponseResult<List<Address>> listMine() {
        return ResponseResult.okResult(addressService.listMine());
    }

    @PostMapping
    public ResponseResult<Void> insert(@RequestBody Address address) {
        addressService.addAddress(address);
        return ResponseResult.okResult();
    }

    @PutMapping
    public ResponseResult<Void> update(@RequestBody Address address) {
        addressService.updateAddress(address);
        return ResponseResult.okResult();
    }

    @DeleteMapping("/{id}")
    public ResponseResult<Void> delete(@PathVariable Long id) {
        addressService.deleteAddress(id);
        return ResponseResult.okResult();
    }

    /** 设为默认地址 */
    @PutMapping("/{id}/default")
    public ResponseResult<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id);
        return ResponseResult.okResult();
    }
}
