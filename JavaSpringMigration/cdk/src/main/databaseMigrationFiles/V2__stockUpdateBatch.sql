DELIMITER $$
CREATE PROCEDURE `batch_product_stock`()
BEGIN
	DECLARE farm_id INTEGER;
    DECLARE product_id INTEGER;
	DECLARE product_name varchar(30);
    DECLARE descr varchar(50);
    DECLARE weight float;
    DECLARE price float;
    DECLARE finished INTEGER DEFAULT(0);

	DECLARE c_stock CURSOR FOR
		select a.farm_id farm_id, a.id product_id, a.name product_name,
        a.description descr, b.weight  weight, b.price price
		from product a, pricing b where b.product_id = a.id;
	DECLARE CONTINUE HANDLER
		FOR NOT FOUND SET finished = 1;

	SELECT 'Wiping out stock_batch table';
    DELETE FROM stock_batch; -- wipe out the table first

	OPEN c_stock;
    SELECT 'Fetching data into stock_batch table';
    getStock: LOOP
		FETCH c_stock INTO farm_id, product_id, product_name, descr, weight, price;

        INSERT INTO stock_batch VALUES (farm_id, product_id, product_name, descr, weight, price);
        IF finished = 1 THEN
			LEAVE getStock;
		END IF;
	END LOOP getStock;

    CLOSE c_stock;
    SELECT "Done writing to stock_batch";

END$$
DELIMITER ;