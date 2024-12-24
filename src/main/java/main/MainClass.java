package main;

import bundle_service_for_backend.*;
import bundle_system.db_query_system.*;
import bundle_system.io.*;
import bundle_system.io.sql.*;
import bundle_system.memory_query_system.*;
import org.xml.sax.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.xpath.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

import static bundle_system.io.SharedAttributes.*;

public class MainClass {
    public static void main(String[] args) throws  InterruptedException {
        SQLUtils sqlUtils = new SQLUtils();
        BackendBundleSystem backendBundleSystem = new BackendBundleSystem(8,sqlUtils,51);
        backendBundleSystem.test();
    }
}
