package br.feevale.fevasDB;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

public class Persistor {

	private static final int INSERIR = 0;
	private static final int ALTERAR = 1;
	private static final int EXCLUIR = 2;

	private Object tbl;
	private int qtRegistrosAfetados;
	private ClasseDeNegocio cn;

	private boolean showCommand = true;

	public Persistor( Object tbl ) {
		this.tbl = tbl;
	}

	public void insere() throws FevasDBException {

		Conexao cnx = PoolDeConexoes.getConexao();
		
		try {
			executaRegrasAntes( INSERIR, cnx );
			StringBuilder cmd = new StringBuilder();
			StringBuilder prm = new StringBuilder();
	
			String nomeTabela = tbl.getClass().getSimpleName();
			Table tn = tbl.getClass().getAnnotation( Table.class );
	
			if( tn != null ) {
				nomeTabela = tn.name();
			}
	
			cmd.append( "insert into " );
			cmd.append( nomeTabela );
			cmd.append( " (" );
	
			Field[] campos = tbl.getClass().getDeclaredFields();
			Object[] vlrs = new Object[ campos.length ];
			ArrayList<Object[]> autoIncrements = null;
			int nrPrms = 0;
	
			for( Field campo : campos ) {
	
				if( !campo.isAnnotationPresent( AutoIncrement.class ) ) {
	
					Object vlr = getValorCampo( campo.getName() );
	
					cmd.append( campo.getName() );
					cmd.append( ", " );
					prm.append( vlr != null ? "?" : "null" );
					prm.append( ", " );
	
					vlrs[ nrPrms++ ] = vlr;
				} else {
					if( autoIncrements == null ) {
						autoIncrements = new ArrayList<Object[]>();
					}
					
					autoIncrements.add( new Object[] { campo.getName(), campo.getType() } );
				}
			}
	
			cmd.delete( cmd.length() - 2, cmd.length() );
			cmd.append( " ) values ( " );
			cmd.append( prm );
			cmd.delete( cmd.length() - 2, cmd.length() );
			cmd.append( " );" );
	
			if( showCommand ) System.out.println( cmd.toString() );
			String[] autoInc = null;
			
			if( autoIncrements != null ) {
				autoInc = new String[ autoIncrements.size() ];
				
				int k = 0;
				for( Object[] autoField : autoIncrements ) {
					autoInc[ k++ ] = autoField[ 0 ].toString().toLowerCase();
				}
			}

			PreparedStatement ps = cnx.getPreparedStatement( cmd.toString(), autoInc );
			
			try {

				int i = 1;
				for( int np = 0; np < nrPrms; np++ ) {

					Object vl = vlrs[ np ];
					if( vl != null ) {
						ps.setObject( i++, vl );
					}
				}

				if( showCommand ) System.out.println( ps.toString() );

				ps.execute();
				
				if( autoIncrements != null ) {
					recuperaCamposAutoIncrementados( ps.getGeneratedKeys(), autoIncrements );
				}

				executaRegrasDepois( INSERIR, cnx );
			} finally {
				cnx.libera();
			}
		} catch( Exception e ) {
			throw new FevasDBException( e );
		}
	}

	private void recuperaCamposAutoIncrementados( ResultSet rs, ArrayList<Object[]> autoIncrements ) throws SQLException {

		if( rs.next() ) {
			
			int indColuna = 1;
			for( Object[] autoInfo : autoIncrements ) {

				String field = (String) autoInfo[ 0 ];
				Class<?> tipo = (Class<?>) autoInfo[ 1 ];
				
				String nomeMetodoSet = "set" + Character.toUpperCase( field.charAt( 0 ) ) + field.substring( 1 );
				Object vlr = rs.getObject( indColuna++ );
				
				if( vlr != null ) {
					
					try {
						Method mtd = tbl.getClass().getDeclaredMethod( nomeMetodoSet, tipo );
						
						try {
							mtd.invoke( tbl, vlr );
						} catch( Exception e ) {
							if( vlr.getClass().getSimpleName().equals( "BigDecimal" ) ) {
								mtd.invoke( tbl, ((BigDecimal) vlr).doubleValue() ); 
							} else if( vlr.getClass().getSimpleName().equals( "Timestamp" ) && tipo.getSimpleName().equals( "Date" ) ) {
								mtd.invoke( tbl, new Date( ((Timestamp) vlr).getTime() ) );
							}
						}
					} catch( Exception e ) {
						System.out.println( "Não sei atribuir o atributo: " + field + " - " + tipo.getSimpleName() + "/" + vlr.getClass().getSimpleName() );
						System.out.println( tipo + " - " + vlr.getClass() );
						e.printStackTrace();
					}
				}
			}
		}
	}

	private Object getValorCampo( String nomeCampo ) throws FevasDBException {

		String nomeMetodoGet = "get" + Character.toUpperCase( nomeCampo.charAt( 0 ) ) + nomeCampo.substring( 1 );

		try {
			Method m = tbl.getClass().getDeclaredMethod( nomeMetodoGet, (Class[]) null );
			Object vl = m.invoke( tbl, (Object[]) null );

			return vl;

		} catch( Exception e ) {
			throw new FevasDBException( e );
		}
	}

	public void deleta() throws FevasDBException {

		Conexao cnx = PoolDeConexoes.getConexao();
		
		try {
			executaRegrasAntes( EXCLUIR, cnx );
	
			StringBuilder cmd = new StringBuilder();
	
			String nomeTabela = tbl.getClass().getSimpleName();
			Table tn = tbl.getClass().getAnnotation( Table.class );
	
			if( tn != null ) {
				nomeTabela = tn.name();
			}
	
			cmd.append( "DELETE FROM " );
			cmd.append( nomeTabela );
			cmd.append( " WHERE " );
	
			Field[] campos = tbl.getClass().getDeclaredFields();
			ArrayList<Object> vlrs = new ArrayList<Object>();
			int i = 0;
	
			for( Field campo : campos ) {
	
				if( campo.isAnnotationPresent( PrimaryKey.class ) ) {
	
					cmd.append( campo.getName() );
					cmd.append( " = ? and " );
	
					vlrs.add( getValorCampo( campo.getName() ) );
				}
			}
	
			cmd.delete( cmd.length() - 4, cmd.length() );

			PreparedStatement ps = cnx.getPreparedStatement( cmd.toString() );

			try {

				i = 1;
				for( Object vl : vlrs ) {
					if( vl != null ) {
						ps.setObject( i++, vl );
					}
				}

				if( showCommand ) System.out.println( ps.toString() );
				qtRegistrosAfetados = ps.executeUpdate();

				executaRegrasDepois( EXCLUIR, cnx );
			} catch( Exception e ) {
				throw new FevasDBException( e );
			}
		} finally {
			cnx.libera();
		}
	}

	public void altera() throws FevasDBException {

		Conexao cnx = PoolDeConexoes.getConexao();
		
		try { 
			executaRegrasAntes( ALTERAR, cnx );
	
			StringBuilder cmd = new StringBuilder();
			StringBuilder prm = new StringBuilder();
	
			String nomeTabela = tbl.getClass().getSimpleName();
			Table tn = tbl.getClass().getAnnotation( Table.class );
	
			if( tn != null ) {
				nomeTabela = tn.name();
			}
	
			cmd.append( "UPDATE " );
			cmd.append( nomeTabela );
			cmd.append( " SET " );
	
			Field[] campos = tbl.getClass().getDeclaredFields();
			ArrayList<Object> chaves = new ArrayList<Object>();
			Object[] vlrs = new Object[ campos.length ];
	
			int i = 0;
	
			for( Field campo : campos ) {
	
				Object vlr = getValorCampo( campo.getName() );
				vlrs[ i++ ] = vlr;
	
				cmd.append( campo.getName() );
				cmd.append( " = " );
				cmd.append( vlr == null ? "null" : "?" );
				cmd.append( ", " );
	
				if( campo.isAnnotationPresent( PrimaryKey.class ) ) {
	
					prm.append( campo.getName() );
					prm.append( " = ? and " );
	
					chaves.add( vlr );
				}
			}
	
			cmd.delete( cmd.length() - 2, cmd.length() );
			prm.delete( prm.length() - 4, prm.length() );
	
			cmd.append( " WHERE " );
			cmd.append( prm );
	
			if( showCommand ) System.out.println( cmd.toString() );

			PreparedStatement ps = cnx.getPreparedStatement( cmd.toString() );

			try {

				i = 1;
				for( Object vl : vlrs ) {
					if( vl != null ) {
						ps.setObject( i++, vl );
					}
				}

				for( Object vl : chaves ) {
					if( vl != null ) {
						ps.setObject( i++, vl );
					}
				}

				if( showCommand ) System.out.println( ps.toString() );
				qtRegistrosAfetados = ps.executeUpdate();

				executaRegrasDepois( ALTERAR, cnx );
			} catch( Exception e ) {
				throw new FevasDBException( e );
			}
		} finally {
			cnx.libera();
		}
	}

	public int getQtRegistrosAfetados() {
		return qtRegistrosAfetados;
	}

	private void executaRegrasAntes( int tipoOperacao, Conexao cnx ) throws FevasDBException {

		String nome = tbl.getClass().getName() + "Negocio";

		try {
			Class<?> c = Class.forName( nome );

			Object cno = c.newInstance();

			if( cno instanceof ClasseDeNegocio ) {
				cn = (ClasseDeNegocio) cno;

				switch( tipoOperacao ) {
					case INSERIR:
						cn.beforeInsert( cnx, tbl );
						break;
					case ALTERAR:
						cn.beforeUpdate( cnx, tbl );
						break;
					case EXCLUIR:
						cn.beforeDelete( cnx, tbl );
						break;
				}
			}

		} catch( ClassNotFoundException ex ) {
			// nada a fazer
		} catch( IllegalAccessException ex ) {
			// nada a fazer
		} catch( InstantiationException ex ) {
			// nada a fazer
		}
	}

	private void executaRegrasDepois( int tipoOperacao, Conexao cnx ) throws FevasDBException {

		if( cn != null ) {
			switch( tipoOperacao ) {
				case INSERIR:
					cn.afterInsert( cnx, tbl );
					break;
				case ALTERAR:
					cn.afterUpdate( cnx, tbl );
					break;
				case EXCLUIR:
					cn.afterDelete( cnx, tbl );
					break;
			}
		}
	}
}
