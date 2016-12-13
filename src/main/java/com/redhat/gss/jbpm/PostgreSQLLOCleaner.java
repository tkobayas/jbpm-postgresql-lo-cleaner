package com.redhat.gss.jbpm;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class PostgreSQLLOCleaner {

    public static Connection conn;

    public static List<Long> activeLoidList = new ArrayList<Long>();
    public static List<Long> pgLoidList = new ArrayList<Long>();

    public static void main( String[] args ) throws Exception {

        System.out.println( "Starting PostgreSQLLOCleaner " + new Date() );
        System.out.println( "------------------" );

        Properties properties = new Properties();
        BufferedInputStream inputStream = new BufferedInputStream( new FileInputStream( "jbpm-postgresql-lo-cleaner.properties" ) );
        properties.load( inputStream );
        inputStream.close();

        Class.forName( properties.getProperty( "driverClass" ) );
        conn = DriverManager.getConnection( properties.getProperty( "connectionUrl" ), properties.getProperty( "username" ),
                properties.getProperty( "password" ) );

        collectActiveLoid();

        checkPGLargeObjectTable();

        System.out.println( "------------------------------" );

        System.out.println( "=== activeLoidList" );
        Collections.sort( activeLoidList );
        System.out.println( activeLoidList );

        System.out.println( "=== pgLoidList" );
        Collections.sort( pgLoidList );
        System.out.println( pgLoidList );

        List<Long> LoidListToDelete = new ArrayList<Long>( pgLoidList );
        LoidListToDelete.removeAll( activeLoidList );

        System.out.println( "=== LoidListToDelete" );
        System.out.println( LoidListToDelete );

        Boolean delete = Boolean.parseBoolean( System.getProperty( "delete.enabled", "false" ) );
        if ( delete ) {
            System.out.println( "Delete orphaned large objects" );
            deleteOrphanedLargeObjects( LoidListToDelete );
        } else {
            System.out.println( "Don't delete. Analyze only" );
        }

        System.out.println( "finished" );
        System.out.println();

    }

    private static void deleteOrphanedLargeObjects( List<Long> LoidListToDelete ) {

        for ( Long loid : LoidListToDelete ) {
            runLoUnlink( loid );
        }
    }

    private static void runLoUnlink( Long loid ) {
        Statement stmt = null;
        ResultSet rs = null;

        try {
            String query = "select lo_unlink(" + loid + ")";
            System.out.println( query );

            stmt = conn.createStatement();
            rs = stmt.executeQuery( query );

        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        } finally {
            try {
                if ( rs != null ) {
                    rs.close();
                }
                if ( stmt != null ) {
                    stmt.close();
                }
            } catch ( SQLException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private static void checkPGLargeObjectTable() {

        List<Long> result = runQuery( "select distinct loid from pg_largeobject" );
        pgLoidList = result;
    }

    private static void collectActiveLoid() {
        // BLOB
        collectActiveLoidPerColumn( "select content from content" );
        collectActiveLoidPerColumn( "select processinstancebytearray from processinstanceinfo" );
        collectActiveLoidPerColumn( "select requestdata from requestinfo" );
        collectActiveLoidPerColumn( "select responsedata from requestinfo" );
        collectActiveLoidPerColumn( "select rulesbytearray from sessioninfo" );
        collectActiveLoidPerColumn( "select workitembytearray from workiteminfo" );

        // CLOB
        collectActiveLoidPerColumn( "select expression from booleanexpression" );
        collectActiveLoidPerColumn( "select body from email_header" );
        collectActiveLoidPerColumn( "select text from i18ntext" );
        collectActiveLoidPerColumn( "select text from task_comment" );
        collectActiveLoidPerColumn( "select qexpression from querydefinitionstore" );
        collectActiveLoidPerColumn( "select deploymentunit from deploymentstore" );

    }

    private static void collectActiveLoidPerColumn( String query ) {
        List<Long> result = runQuery( query );
        activeLoidList.addAll( result );
    }

    private static List<Long> runQuery( String query ) {

        System.out.println( query );

        Statement stmt = null;
        ResultSet rs = null;

        List<Long> result = new ArrayList<Long>();

        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery( query );

            while ( rs.next() ) {
                Object oid = rs.getObject( 1 );
                if ( oid instanceof Long ) {
                    result.add( (Long) oid );
                } else if ( oid instanceof String ) {
                    result.add( Long.parseLong( (String) oid ) );
                } else {
                    System.out.println( "##### " + oid.getClass() );
                }
            }

        } catch ( SQLException e ) {
            String message = e.getMessage();
            if ( message.contains( "relation" ) && message.contains( "does not exist" ) ) {
                // if the table doesn't exist, ignore
                System.out.println( message );
            } else {
                throw new RuntimeException( e );
            }
        } finally {
            try {
                if ( rs != null ) {
                    rs.close();
                }
                if ( stmt != null ) {
                    stmt.close();
                }
            } catch ( SQLException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        System.out.println( result );

        return result;
    }

}