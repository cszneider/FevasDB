package br.feevale.fevasDB;

public interface ClasseDeNegocio {
    
    public void beforeInsert( Conexao cnx, Object registro ) throws FevasDBException;
    public void beforeUpdate( Conexao cnx, Object registro ) throws FevasDBException;
    public void beforeDelete( Conexao cnx, Object registro ) throws FevasDBException;
    
    public void afterInsert( Conexao cnx, Object registro ) throws FevasDBException;
    public void afterUpdate( Conexao cnx, Object registro ) throws FevasDBException;
    public void afterDelete( Conexao cnx, Object registro ) throws FevasDBException;
}
