package com.monframework.core;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public class FrontControllerServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String context = req.getContextPath(); // /TestProject
        String uri = req.getRequestURI(); // /TestProject/hello
        String path = uri.substring(context.length());

        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().println("URL demandée : " + path);
        resp.getWriter().println("Méthode HTTP : " + req.getMethod());
    }
}
