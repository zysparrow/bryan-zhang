package org.poseidon.service.system.impl;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.poseidon.global.PoseidonSession;
import org.poseidon.listener.OnLineUserSessionBindingListener;
import org.poseidon.pojo.Login;
import org.poseidon.service.system.LoginService;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

@Component("loginService")
public class LoginServiceImpl implements LoginService {
	@Resource(name = "hibernateTemplate")
	private HibernateTemplate hibernateTemplate;
	@Resource(name = "onLineUserSessionBindingListener")
	HttpSessionBindingListener onLineUserSessionBindingListener;

	@SuppressWarnings({ "unchecked", "deprecation" })
	public String returnLoginMessage(String loginEmail, String password) throws Exception {
		String loginMessage = "success";
		DetachedCriteria dc =  DetachedCriteria.forClass(Login.class);
		dc.add(Restrictions.sqlRestriction("upper({alias}.login_email) = ?", loginEmail.toUpperCase(), Hibernate.STRING));
		dc.add(Restrictions.eq("loginPassword", password));
		List<Login> loginList = hibernateTemplate.findByCriteria(dc);
		
		if (loginList.size() == 0) {
			loginMessage = "wrongLogin";
		} else if ("0".equals(loginList.get(0).getIsAvail())) {
			loginMessage = "invalidLogin";
		} else if (OnLineUserSessionBindingListener.isLogin(loginEmail)) {
			loginMessage = "hasLogined";
		}
		return loginMessage;
	}

	public void setSession(String loginEmail,HttpServletRequest request,HttpServletResponse response) throws Exception {
		Login login = this.getLogin(loginEmail);
		if(login==null){
			return;
		}
		String isSaveLoginInfo = request.getParameter("isSaveLoginInfo");
		if (StringUtils.isNotEmpty(isSaveLoginInfo)){
			Cookie c = null;
			c = new Cookie("loginEmail",login.getLoginEmail());
			c.setMaxAge(60000);
			response.addCookie(c);
			c = new Cookie("loginPassword",login.getLoginPassword());
			c.setMaxAge(60000);
			response.addCookie(c);
		}
		
		PoseidonSession poseidonSession = new PoseidonSession();
		poseidonSession.setLogin(login);
		HttpSession session = request.getSession();
		session.setAttribute("poseidonSession", poseidonSession);
		session.setAttribute(poseidonSession.getLogin().getLoginEmail(), onLineUserSessionBindingListener);
	}
	
	public void addLogin(Login login) throws Exception{
		hibernateTemplate.save(login);
	}
	
	/***************************************************private method****************************************************/

	@SuppressWarnings({ "unchecked", "deprecation" })
	private Login getLogin(String loginEmail) {
		DetachedCriteria dc =  DetachedCriteria.forClass(Login.class);
		dc.add(Restrictions.sqlRestriction("upper({alias}.login_email) = ?", loginEmail.toUpperCase(), Hibernate.STRING));
		List<Login> loginList = hibernateTemplate.findByCriteria(dc);
		Login login = null;
		if (!(loginList.isEmpty())) {
			login = loginList.get(0);
		}
		return login;
	}
	
	
}
