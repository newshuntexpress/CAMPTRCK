package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Employee
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY updatedAt DESC")
    fun getAllEmployeesFlow(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE status = 'ACTIVE' ORDER BY roomNumber ASC, bedNumber ASC")
    fun getActiveEmployeesFlow(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE status = 'ACTIVE' AND roomNumber = :roomNumber")
    suspend fun getActiveEmployeesForRoom(roomNumber: String): List<Employee>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getEmployeeById(id: Long): Employee?

    @Query("SELECT * FROM employees WHERE iqamaNumber = :iqamaNumber LIMIT 1")
    suspend fun getEmployeeByIqama(iqamaNumber: String): Employee?

    @Query("SELECT * FROM employees WHERE cardNumber = :cardNumber LIMIT 1")
    suspend fun getEmployeeByCard(cardNumber: String): Employee?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(employees: List<Employee>)

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Delete
    suspend fun deleteEmployee(employee: Employee)

    @Query("""
        SELECT * FROM employees 
        WHERE (name LIKE '%' || :query || '%' 
           OR iqamaNumber LIKE '%' || :query || '%' 
           OR cardNumber LIKE '%' || :query || '%' 
           OR roomNumber LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    fun searchEmployeesFlow(query: String): Flow<List<Employee>>

    @Query("SELECT COUNT(*) FROM employees")
    suspend fun getTotalEmployeesCount(): Int

    @Query("UPDATE employees SET company = :companyName WHERE company != :companyName")
    suspend fun updateAllCompanyNames(companyName: String)
}
