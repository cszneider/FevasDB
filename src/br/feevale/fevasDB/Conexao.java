package br.feevale.fevasDB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Conexao {

	private Connection cnx;
	private boolean livre;

	public Conexao() throws FevasDBException {

		Parametros prm = Parametros.getInstance();
		livre = true;

		try {
			Class.forName( prm.getParametro( "driverJDBC" ) );

			String baseUrl = prm.getParametro( "baseURL" );
			String endBanco = prm.getParametro( "endBanco" );
			String nroPorta = prm.getParametro( "nroPorta" );
			String nomeDatabase = prm.getParametro( "nomeDatabase" );

			String urlBanco = baseUrl + endBanco + ":" + nroPorta + "/" + nomeDatabase;
			String nome = prm.getParametro( "nomeUsuario" );
			String senha = prm.getParametro( "senhaUsuario" );

			cnx = DriverManager.getConnection( urlBanco, nome, senha );
		} catch( Exception e ) {
			throw new FevasDBException( e );
		}

	}

	public void beginTransaction() throws FevasDBException {

		try {
			Statement st = cnx.createStatement();
			st.execute( "start transaction;" );
		} catch( SQLException e ) {
			throw new FevasDBException( e );
		}
	}

	public void commit() throws FevasDBException {

		try {
			Statement st = cnx.createStatement();
			st.execute( "commit;" );
		} catch( SQLException e ) {
			throw new FevasDBException( e );
		}
	}

	public void rollback() throws FevasDBException {

		try {
			Statement st = cnx.createStatement();
			st.execute( "rollback;" );
		} catch( SQLException e ) {
			throw new FevasDBException( e );
		}
	}

	public void executaComandoSQL( String comando ) throws FevasDBException {

		try {
			Statement st = cnx.createStatement();
			st.execute( comando );
		} catch( SQLException e ) {
			throw new FevasDBException( e );
		}
	}

	public PreparedStatement getPreparedStatement( String cmdSql ) throws FevasDBException {

		try {
			return cnx.prepareStatement( cmdSql );
		} catch( SQLException e ) {
			throw new FevasDBException( e );
		}
	}
	
	public ResultSet executaQuery( String cmdSQL ) throws FevasDBException {

		try {
			Statement st = cnx.createStatement();
			return st.executeQuery( cmdSQL );
		} catch (Exception e) {
			throw new FevasDBException( e );
		}
	}

	public void insereRegistro( String nomeTabela, String[] nomesCampos, Object[] valoresCampos ) throws FevasDBException {

		if( nomesCampos.length != valoresCampos.length ) {
			throw new FevasDBException( "Quantidade de campos difere da quantidade de valores" );
		}

		StringBuilder cmd = new StringBuilder( "insert into " );
		StringBuilder prm = new StringBuilder();

		cmd.append( nomeTabela );
		cmd.append( " ( " );

		for( int i = 0; i < nomesCampos.length; i++ ) {

			if( prm.length() != 0 ) {
				cmd.append( ", " );
				prm.append( ", " );
			}

			cmd.append( nomesCampos[ i ] );
			prm.append( "?" );
		}

		cmd.append( " ) values ( " );
		cmd.append( prm );
		cmd.append( " );" );

		PreparedStatement ps = getPreparedStatement( cmd.toString() );
		int i = 1;

		try {
			for( Object vl : valoresCampos ) {
				ps.setObject( i++, vl );
			}

			ps.execute();
		} catch( SQLException e ) {
			throw new FevasDBException( e );
		}
	}

	public Connection getConnection() {
		return cnx;
	}

	public void desconecta() {
		try {
			cnx.close();
		} catch( SQLException ex ) {
			// nada a fazer
		}
	}
	
	public void reserva() throws FevasDBException {

		if( livre ) {
			livre = false;
		} else {
			throw new FevasDBException( "Conex‹o j‡ em uso!" );
		}
	}
	
	public void libera() {
		livre = true;
	}

	public boolean isLivre() {
		return livre;
	}
}
