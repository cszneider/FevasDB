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
import java.util.List;

public class Localizador <T extends Object> {
	
	private T registro;
	private boolean found;
	private Class<T> c;
	
	private boolean showCommand = true;
	private boolean human = false;
	
	public Localizador( Class<T> c ) {
		this.c = c;
		found = false;
	}
	
	public boolean localiza( Object... id ) throws FevasDBException {
		
		try {
			found = false;

			Field[] fields = c.getDeclaredFields();
			StringBuilder cmd = new StringBuilder( "select " );
			StringBuilder where = new StringBuilder( " where " );
			
			int qtChaves = 0;
			
			for( Field f : fields ) {
				cmd.append( f.getName() );
				cmd.append( ", " );
				
				if( f.isAnnotationPresent( PrimaryKey.class ) ) {
					where.append( f.getName() );
					where.append( " = ?" );
					where.append( " and " );
					
					qtChaves++;
				}
			}
			
			if( qtChaves != id.length ) {
				throw new FevasDBException( "Quantidade de chaves difere das chaves fornecidas" );
			}
			
			cmd.delete( cmd.length() - 2, cmd.length() );		// retiro ", " final
			where.delete( where.length() - 5, where.length() ); // retiro " and " final
			
			Table tn = c.getAnnotation( Table.class );
			cmd.append( " from " );
			cmd.append( tn != null ? tn.name() : c.getSimpleName() );
			cmd.append( where );
			
			Conexao cnx = PoolDeConexoes.getConexao();

			try {
				
				PreparedStatement ps = cnx.getPreparedStatement( cmd.toString() );
				
				for( int i = 0; i < qtChaves; i++ ) {
					ps.setObject( i + 1, id[ i ] );
				}
				
				ResultSet rs = ps.executeQuery();
				
				if( rs.next() ) {
					
					registro = c.newInstance();							// crio uma nova instância de registro.
					preencheRegistro( registro, rs );
					
					found = true;
				}
			} finally {
				cnx.libera();
			}
		} catch( Exception e ) {
			throw new FevasDBException( e );
		}
		
		return isFound();
	}

	public boolean localizaPorAlternateKey( String alternateKeyName, Object... id ) throws FevasDBException {
		
		try {
			found = false;

			Field[] fields = c.getDeclaredFields();
			StringBuilder cmd = new StringBuilder( "select " );
			StringBuilder where = new StringBuilder( " where " );
			
			int qtChaves = 0;
			
			for( Field f : fields ) {
				cmd.append( f.getName() );
				cmd.append( ", " );
				
				AlternateKey ak = f.getAnnotation( AlternateKey.class );
				
				if( ak != null && ak.keyName().equals( alternateKeyName ) ) {
					where.append( f.getName() );
					where.append( " = ?" );
					where.append( " and " );
					
					qtChaves++;
				}
			}
			
			if( qtChaves != id.length ) {
				throw new FevasDBException( "Quantidade de chaves difere das chaves fornecidas" );
			}
			
			cmd.delete( cmd.length() - 2, cmd.length() );		// retiro ", " final
			where.delete( where.length() - 5, where.length() ); // retiro " and " final
			
			Table tn = c.getAnnotation( Table.class );
			cmd.append( " from " );
			cmd.append( tn != null ? tn.name() : c.getSimpleName() );
			cmd.append( where );
			
			Conexao cnx = PoolDeConexoes.getConexao();

			try {
				
				PreparedStatement ps = cnx.getPreparedStatement( cmd.toString() );
				
				for( int i = 0; i < qtChaves; i++ ) {
					ps.setObject( i + 1, id[ i ] );
				}
				
				ResultSet rs = ps.executeQuery();
				
				if( rs.next() ) {
					
					registro = c.newInstance();							// crio uma nova inst√¢ncia de registro.
					preencheRegistro( registro, rs );
					
					found = true;
				}
			} finally {
				cnx.libera();
			}
		} catch( Exception e ) {
			throw new FevasDBException( e );
		}
		
		return isFound();
	}
	
	private void preencheRegistro( T registro, ResultSet rs ) throws SQLException {
		
		Field[] fields = c.getDeclaredFields();
		int indColuna = 1;
		
		for( Field f : fields ) {

			String nomeMetodoSet = "set" + Character.toUpperCase( f.getName().charAt( 0 ) ) + f.getName().substring( 1 );
			Object vlr = rs.getObject( indColuna++ );
			
			if( vlr != null ) {
				
				try {
					Method mtd = registro.getClass().getDeclaredMethod( nomeMetodoSet, f.getType() );
					
					try {
						mtd.invoke( registro, vlr );
					} catch( Exception e ) {
						if( vlr.getClass().getSimpleName().equals( "BigDecimal" ) ) {
							mtd.invoke( registro, ((BigDecimal) vlr).doubleValue() ); 
						} else if( vlr.getClass().getSimpleName().equals( "Timestamp" ) && f.getType().getSimpleName().equals( "Date" ) ) {
							mtd.invoke( registro, new Date( ((Timestamp) vlr).getTime() ) );
						}
					}
				} catch( Exception e ) {
					System.out.println( "N�o sei atribuir o atributo: " + f.getName() + " - " + f.getType().getSimpleName() + "/" + vlr.getClass().getSimpleName() );
					System.out.println( f.getType() + " - " + vlr.getClass() );
					e.printStackTrace();
				}
			}
		}
	}
	
	public List<T> localiza() throws FevasDBException {
		return localiza( (String) null, (Object[]) null );
	}
	        
	public List<T> localiza( String clausulaWhere, Object...camposSelecao ) throws FevasDBException {
		return localiza( clausulaWhere, camposSelecao, null, null, null );
	}
	
	public List<T> localiza( String clausulaWhere, Object[] camposSelecao, String orderBy ) throws FevasDBException {
		return localiza( clausulaWhere, camposSelecao, orderBy, null, null );
	}
	
	public List<T> localiza( String clausulaWhere, Object[] camposSelecao, String orderBy, Integer limit ) throws FevasDBException {
		return localiza( clausulaWhere, camposSelecao, orderBy, limit, null );
	}
	
	public List<T> localiza( String clausulaWhere, Object[] camposSelecao, Integer limit ) throws FevasDBException {
		return localiza( clausulaWhere, camposSelecao, null, limit, null );
	}
	
	public List<T> localiza( String clausulaWhere, Object[] camposSelecao, Integer limit, Integer offSet ) throws FevasDBException {
		return localiza( clausulaWhere, camposSelecao, null, limit, offSet );
	}
	
	public List<T> localiza( String clausulaWhere, Object[] camposSelecao, String orderBy, Integer limit, Integer offSet ) throws FevasDBException {

		StringBuilder cmd = new StringBuilder( "select " );

		try {
			ArrayList<T> result = new ArrayList<T>();
			
			Field[] fields = c.getDeclaredFields();

			for( Field f : fields ) {
				ColumnAlias o = f.getAnnotation( ColumnAlias.class );
				if( o != null ) {
					cmd.append( o.value() );
					cmd.append( "." );
				}
				
				ColumnName cn = f.getAnnotation( ColumnName.class );
				cmd.append( cn != null ?  cn.value() : f.getName() );
				cmd.append( ", " );
			}
			
			cmd.delete( cmd.length() - 2, cmd.length() );
			if( human ) cmd.append( "\n" );

			Table tn = c.getAnnotation( Table.class );
			cmd.append( " from " );
			if( tn == null ) {
				cmd.append( c.getSimpleName() );
			} else {
				cmd.append( tn.name() );
				
				if( tn.alias().trim().length() > 0 ) {
					cmd.append( " " );
					cmd.append( tn.alias() );
				}
			}

			if( human ) cmd.append( "\n" );
			ArrayList<JoinRule> joins = getJoins();
			
			if( joins != null ) {
				addJoins( joins, cmd );
			}

			if( clausulaWhere != null ) {
				
				clausulaWhere = clausulaWhere.trim();
				
				if( !clausulaWhere.toLowerCase().startsWith( "where " ) ) {
					cmd.append( " where " );
				}
				
				cmd.append( clausulaWhere );
			}
                        
                        if (orderBy != null) {
                            cmd.append(" order By ");
                            cmd.append( orderBy );
                        }
			
			if( limit != null ) {
				cmd.append( " limit " );
				cmd.append( limit );
			}
			
			if( offSet != null ) {
				cmd.append( " offset " );
				cmd.append( offSet );
			}

			Conexao cnx = PoolDeConexoes.getConexao();
			
			try {
				
				PreparedStatement ps = cnx.getPreparedStatement( cmd.toString() );
				
				if( camposSelecao != null ) {
					for( int i = 0; i < camposSelecao.length; i++ ) {
						ps.setObject( i + 1, camposSelecao[ i ] );
					}
				}
				
				if( showCommand ) System.out.println( ps.toString() );
				ResultSet rs = ps.executeQuery();
				
				if( rs.next() ) {
					
					while( !rs.isAfterLast() ) {
						
						T registro = c.newInstance();
						preencheRegistro( registro, rs );
						
						result.add( registro );
						rs.next();
					}
				}
			} finally {
				cnx.libera();
			}
			
			return result;
		} catch ( Exception e ) {
			System.out.println( cmd.toString() );
			throw new FevasDBException( e );
		}
	}

	private void addJoins( ArrayList<JoinRule> joins, StringBuilder cmd ) {

		for( JoinRule rule : joins ) {

			cmd.append(  " " );
			
			switch( rule.type() ) {
				case FULL:  cmd.append( "full" ); break;
				case INNER: cmd.append( "inner" ); break;
				case LEFT:  cmd.append( "left" ); break;
				case RIGHT: cmd.append( "right" ); break;
			}

			cmd.append( " join " );
			cmd.append( rule.tableName() );
			
			if( !rule.alias().trim().equals( "" ) ) {
				cmd.append( " " );
				cmd.append( rule.alias() );
			}
			
			cmd.append( " on " );
			cmd.append( rule.condition() );
			if( human ) cmd.append( "\n" );
		}
	}

	private ArrayList<JoinRule> getJoins() {
		
		ArrayList<JoinRule> ar = null;
		JoinRule jr = c.getAnnotation( JoinRule.class );
		
		if( jr != null ) {
			ar = new ArrayList<JoinRule>();
			ar.add( jr );
		}
		
		JoinList jl = c.getAnnotation( JoinList.class );
		
		if( jl != null ) {
			
			if( ar == null ) {
				ar = new ArrayList<JoinRule>();
			}
			
			for( JoinRule rule : jl.value() ) {
				ar.add( rule );
			}
		}
		 
		return ar;
	}

	public boolean isFound() {
		return found;
	}
	
	public T getRegistro() {
		return registro;
	}
	
	public void setShowCommand( boolean showCommand ) {
		this.showCommand = showCommand;
	}
	
	public boolean isShowCommand() {
		return showCommand;
	}
	
	public void setHuman( boolean human ) {
		this.human = human;
	}
	
	public boolean isHuman() {
		return human;
	}
}