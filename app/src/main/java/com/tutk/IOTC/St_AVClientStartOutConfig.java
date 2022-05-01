/******************************************************************************
 *                                                                            *
 * Copyright (c) 2011 by TUTK Co.LTD. All Rights Reserved.                    *
 *                                                                            *
 *                                                                            *
 * Class: St_AVClientStartOutConfig                                                    *
 *                                                                            *
 * Author: Roger                                                              *
 *                                                                            *
 * Date: 2018/05/28                                                           *
 *                                                                            *
 ******************************************************************************/

package com.tutk.IOTC;

//base on the struct AVClientStartOutConfig in AVAPIs.h
public class St_AVClientStartOutConfig
{
    public int server_type;
    public int resend;
    public int two_way_streaming;
    public int sync_recv_data;
    public int security_mode; //0:simple 1:DTLS
}