package com.monstarbill.masters.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monstarbill.masters.feignclient.SetupServiceClient;

@RestController
@RequestMapping("/masters")
public class TestController {
	
	@Autowired
	private SetupServiceClient setupServiceClient;

	@GetMapping("/status/check")
	public String getStatus() {
		return "working good from masters...";
	}
	
	@GetMapping("/status/comm")
	public List<String> getStatusComm(@RequestParam int id, @RequestParam String name) {
		List<String> list = new ArrayList<String>();
		list.add("aaaa");
		list.add("bbb");
		list.add("ccc");
		list.add("ddddd");
		list.add("eee");
		return list;
	}
	
	@GetMapping("/status/check-comm")
	public ResponseEntity<List<String>> getStatusComm() {
		// 2. Communication using Feign Client (Declarative approach)
		List<String> data = new ArrayList<String>();
		data = this.setupServiceClient.getStatusList(1, "aaa");
		return new ResponseEntity<List<String>>(data, HttpStatus.OK);
	}
}
