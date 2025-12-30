package com.bankwave.accounts.service;

import com.bankwave.accounts.dto.CustomerDto;

public interface IAccountsService {
    /**
     *
     * @param customerDto -CustomerDto Object
     */
    void createAccount(CustomerDto customerDto);

    /**
     *
     * @param mobileNumber -Input Mobile Number
     * @retun Account Details based on given mobilenUmber
     */
    CustomerDto fetchAccount(String mobileNumber);

    /**
     *
     * @param customerDto -Input customerDto
     * @retun boolean check if data update is successful
     */
    boolean updateAccount(CustomerDto customerDto);

    /**
     *
     * @param mobileNumber -Input mobileNumber
     * @retun boolean check if data deleted is successful
     */
    boolean deleteAccount(String mobileNumber);

}
