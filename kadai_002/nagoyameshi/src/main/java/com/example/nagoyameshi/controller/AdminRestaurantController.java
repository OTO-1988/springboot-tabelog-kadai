package com.example.nagoyameshi.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nagoyameshi.entity.Category;
import com.example.nagoyameshi.entity.RegularHoliday;
import com.example.nagoyameshi.entity.Restaurant;
import com.example.nagoyameshi.form.RestaurantEditForm;
import com.example.nagoyameshi.form.RestaurantRegisterForm;
import com.example.nagoyameshi.repository.RestaurantRepository;
import com.example.nagoyameshi.service.CategoryRestaurantService;
import com.example.nagoyameshi.service.CategoryService;
import com.example.nagoyameshi.service.RegularHolidayRestaurantService;
import com.example.nagoyameshi.service.RegularHolidayService;
import com.example.nagoyameshi.service.RestaurantService;

@Controller
@RequestMapping("/admin/restaurants")
public class AdminRestaurantController {
	private final RestaurantRepository restaurantRepository;
	private final RestaurantService restaurantService;
	private final CategoryService categoryService;
	private final CategoryRestaurantService categoryRestaurantService;
	private final RegularHolidayService regularHolidayService;
	private final RegularHolidayRestaurantService regularHolidayRestaurantService;

	public AdminRestaurantController(RestaurantRepository restaurantRepository,
			RestaurantService restaurantService,
			CategoryService categoryService,
			CategoryRestaurantService categoryRestaurantService,
			RegularHolidayService regularHolidayService,
			RegularHolidayRestaurantService regularHolidayRestaurantService) {
		this.restaurantRepository = restaurantRepository;
		this.restaurantService = restaurantService;
		this.categoryService = categoryService;
		this.categoryRestaurantService = categoryRestaurantService;
		this.regularHolidayService = regularHolidayService;
		this.regularHolidayRestaurantService = regularHolidayRestaurantService;
	}

	@GetMapping
	public String index(Model model,
			@PageableDefault(page = 0, size = 10, sort = "id", direction = Direction.ASC) Pageable pageable,
			@RequestParam(required = false) String keyword) {
		Page<Restaurant> restaurantPage;
		if (keyword != null && !keyword.isEmpty()) {
			restaurantPage = restaurantRepository.findByNameLike("%" + keyword + "%", pageable);
		} else {
			restaurantPage = restaurantRepository.findAll(pageable);
		}

		model.addAttribute("restaurantPage", restaurantPage);
		model.addAttribute("keyword", keyword);

		return "admin/restaurants/index";
	}

	@GetMapping("/{id}")
	public String show(@PathVariable Integer id, Model model) {
		Restaurant restaurant = restaurantRepository.getReferenceById(id);
		model.addAttribute("restaurant", restaurant);

		return "admin/restaurants/show";
	}

	@GetMapping("/register")
	public String register(Model model) {
		List<Category> categories = categoryService.findAllCategories();
		List<RegularHoliday> regularHolidays = regularHolidayService.findAllRegularHolidays();
		model.addAttribute("restaurantRegisterForm", new RestaurantRegisterForm());
		model.addAttribute("categories", categories);
		model.addAttribute("regularHolidays", regularHolidays);

		return "admin/restaurants/register";
	}

	@PostMapping("/create")
	public String create(@ModelAttribute @Validated RestaurantRegisterForm restaurantRegisterForm,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes,
			Model model) {

		if (bindingResult.hasErrors()) {
			List<Category> categories = categoryService.findAllCategories();
			List<RegularHoliday> regularHolidays = regularHolidayService.findAllRegularHolidays();
			model.addAttribute("restaurantRegisterForm", restaurantRegisterForm);
			model.addAttribute("categories", categories);
			model.addAttribute("regularHolidays", regularHolidays);

		}
		// サービスクラスにデータ保存を依頼
		try {
			restaurantService.create(restaurantRegisterForm);
			redirectAttributes.addFlashAttribute("successMessage", "店舗情報を登録しました。");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "店舗情報の登録に失敗しました。");
		}
		return "redirect:/admin/restaurants";
	}

	@GetMapping("/{id}/edit")
	public String edit(@PathVariable Integer id, Model model) {
		Restaurant restaurant = restaurantRepository.getReferenceById(id);
		List<Integer> categoryIds = categoryRestaurantService.findCategoryIdsByRestaurantOrderByIdAsc(restaurant);
		List<Integer> regularHoridayIds = regularHolidayRestaurantService.findRegularHolidayIdsByRestaurant(restaurant);
		String imageName = restaurant.getImageName();
		RestaurantEditForm restaurantEditForm = new RestaurantEditForm(restaurant.getId(),
				restaurant.getName(),
				null,
				restaurant.getDescription(),
				restaurant.getLowestPrice(),
				restaurant.getHighestPrice(),
				restaurant.getPostalCode(),
				restaurant.getAddress(),
				restaurant.getOpenTime(),
				restaurant.getCloseTime(),
				restaurant.getSeatingCapacity(),
				categoryIds,
				regularHoridayIds);
		List<Category> categories = categoryService.findAllCategories();
		List<RegularHoliday> regularHolidays = regularHolidayService.findAllRegularHolidays();

		model.addAttribute("imageName", imageName);
		model.addAttribute("restaurantEditForm", restaurantEditForm);
		model.addAttribute("categories", categories);
		model.addAttribute("regularHolidays", regularHolidays);

		return "admin/restaurants/edit";
	}

	@PostMapping("/{id}/update")
	public String update(@ModelAttribute @Validated RestaurantEditForm restaurantEditForm,
			BindingResult bindingResult,
			@PathVariable Integer id,
			RedirectAttributes redirectAttributes,
			Model model) {

		if (bindingResult.hasErrors()) {
			Restaurant restaurant = restaurantService.findById(id); // ここでレストランを取得
			List<Category> categories = categoryService.findAllCategories();
			List<RegularHoliday> regularHolidays = regularHolidayService.findAllRegularHolidays();
			model.addAttribute("restaurant", restaurant);
			model.addAttribute("restaurantEditForm", restaurantEditForm);
			model.addAttribute("categories", categories);
			model.addAttribute("regularHorlidays", regularHolidays);
		}

		restaurantService.update(restaurantEditForm);
		redirectAttributes.addFlashAttribute("successMessage", "店舗情報を編集しました。");

		return "redirect:/admin/restaurants";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
		restaurantRepository.deleteById(id);

		redirectAttributes.addFlashAttribute("successMessage", "店舗情報を削除しました。");

		return "redirect:/admin/restaurants";
	}
}
